package com.tlean07.saferoute;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Open-Meteo model estimates, not street-level flood detections. */
@Service
public class WeatherService {
  private final HttpClient client;
  private final ObjectMapper mapper;
  private final URI endpoint;
  private final Clock clock;
  private JsonNode last;
  private Instant fetchedAt;
  private Instant retryAfter = Instant.MIN;

  @org.springframework.beans.factory.annotation.Autowired
  public WeatherService(
      ObjectMapper mapper,
      @Value(
              "${saferoute.weather-url:https://api.open-meteo.com/v1/forecast?latitude=-23.483&longitude=-46.417&current=temperature_2m,precipitation,rain,weather_code&hourly=precipitation_probability,precipitation&forecast_days=2&timezone=UTC}")
          String url) {
    this(mapper, URI.create(url), Clock.systemUTC());
  }

  WeatherService(ObjectMapper mapper, URI endpoint, Clock clock) {
    this.mapper = mapper;
    this.endpoint = endpoint;
    this.clock = clock;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
  }

  public synchronized Map<String, Object> current() {
    Instant now = clock.instant();
    boolean fresh = last != null && fetchedAt.plusSeconds(900).isAfter(now);
    if (!fresh && !retryAfter.isAfter(now)) {
      try {
        var request =
            HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(8))
                .header(
                    "User-Agent", "SafeRoute-FIAP/1.0 (+https://saferoute-gs-2026.onrender.com)")
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
          throw new IllegalStateException("Upstream HTTP " + response.statusCode());
        JsonNode data = mapper.readTree(response.body());
        if (!data.path("current").path("temperature_2m").isNumber()
            || !data.path("current").path("precipitation").isNumber()
            || !data.path("hourly").path("time").isArray())
          throw new IllegalStateException("Resposta meteorológica incompleta");
        Instant validTime =
            LocalDateTime.parse(data.path("current").path("time").asText())
                .toInstant(ZoneOffset.UTC);
        if (validTime.isBefore(now.minusSeconds(10800)) || validTime.isAfter(now.plusSeconds(3600)))
          throw new IllegalStateException("Dados meteorológicos fora da janela atual");
        last = data;
        fetchedAt = now;
        fresh = true;
        retryAfter = now;
      } catch (Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        retryAfter = now.plusSeconds(60);
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Open-Meteo indisponível: {}", e.getClass().getSimpleName());
      }
    }
    var result = new LinkedHashMap<String, Object>();
    boolean usable = last != null && fetchedAt.plusSeconds(7200).isAfter(now);
    result.put("status", fresh ? "CURRENT" : usable ? "STALE" : "UNAVAILABLE");
    result.put("source", "Open-Meteo");
    result.put("sourceUrl", "https://open-meteo.com/");
    result.put("license", "CC BY 4.0");
    result.put("kind", "MODEL_ESTIMATE");
    result.put("timezone", "UTC");
    result.put(
        "message",
        "Estimativa meteorológica regional; chuva prevista não confirma alagamento em uma rua.");
    result.put("fetchedAt", fetchedAt);
    result.put("checkedAt", now);
    result.put("data", usable ? last : null);
    return result;
  }
}
