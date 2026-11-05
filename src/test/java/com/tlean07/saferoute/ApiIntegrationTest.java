package com.tlean07.saferoute;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "saferoute.real-network=false")
@ActiveProfiles("demo")
class ApiIntegrationTest {
  @Autowired TestRestTemplate http;

  HttpHeaders headers() {
    var h = new HttpHeaders();
    h.set("X-Admin-Key", "demo-saferoute-local");
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  <T> ResponseEntity<T> call(String url, HttpMethod method, Object body, Class<T> type) {
    return http.exchange(url, method, new HttpEntity<>(body, headers()), type);
  }

  @Test
  void healthAndGraph() {
    assertEquals(200, http.getForEntity("/api/health", String.class).getStatusCode().value());
    assertEquals(6, http.getForObject("/api/points", Point[].class).length);
    assertTrue(http.getForObject("/api/graph", String.class).contains("simulated"));
  }

  @Test
  void authorizationAndValidation() throws Exception {
    var request =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(http.getRootUri() + "/api/regions"))
            .header("Content-Type", "application/json")
            .POST(
                java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"name\":\"A\",\"city\":\"São Paulo\"}"))
            .build();
    assertEquals(
        401,
        java.net.http.HttpClient.newHttpClient()
            .send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            .statusCode());
    assertEquals(
        400,
        call("/api/regions", HttpMethod.POST, new Region(null, "", ""), String.class)
            .getStatusCode()
            .value());
    assertEquals(
        404, http.getForEntity("/api/regions/999999", String.class).getStatusCode().value());
  }

  @Test
  void completeCrud() {
    var c =
        call(
            "/api/regions",
            HttpMethod.POST,
            new Region(null, "Teste temporário", "São Paulo"),
            Region.class);
    assertEquals(201, c.getStatusCode().value());
    long id = c.getBody().id();
    assertNotNull(c.getHeaders().getLocation());
    assertEquals(
        "Atualizado",
        call(
                "/api/regions/" + id,
                HttpMethod.PUT,
                new Region(null, "Atualizado", "São Paulo"),
                Region.class)
            .getBody()
            .name());
    assertEquals(
        204,
        call("/api/regions/" + id, HttpMethod.DELETE, null, Void.class).getStatusCode().value());
    assertEquals(
        404, http.getForEntity("/api/regions/" + id, String.class).getStatusCode().value());
  }

  @Test
  void foreignKeyProtection() {
    assertEquals(
        409, call("/api/regions/1", HttpMethod.DELETE, null, String.class).getStatusCode().value());
    assertEquals(
        400,
        call(
                "/api/segments",
                HttpMethod.POST,
                new Segment(null, 1L, 1L, "Inválido", 10, 1, 1, 0),
                String.class)
            .getStatusCode()
            .value());
  }

  @Test
  void accessibleDetour() {
    var r =
        http.postForObject(
            "/api/routes", new RouteBO.Request(1L, 3L, true, true), RouteBO.Result.class);
    assertEquals("FOUND", r.status());
    assertEquals(List.of(1L, 4L, 5L, 6L, 3L), r.points());
    assertEquals(1200, r.meters());
    assertFalse(r.segments().contains(1L));
  }

  @Test
  void missingAndUnreachable() {
    assertEquals(
        404,
        http.postForEntity(
                "/api/routes", new RouteBO.Request(999999L, 3L, false, false), String.class)
            .getStatusCode()
            .value());
    var r =
        call("/api/points", HttpMethod.POST, new Point(null, 1L, "Isolado", 900, 900), Point.class)
            .getBody();
    try {
      assertEquals(
          "NO_ROUTE",
          http.postForObject(
                  "/api/routes", new RouteBO.Request(1L, r.id(), true, false), RouteBO.Result.class)
              .status());
    } finally {
      call("/api/points/" + r.id(), HttpMethod.DELETE, null, Void.class);
    }
  }

  @Test
  void occurrenceLifecycleAndAlert() {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var o =
        new Occurrence(
            null,
            3L,
            "OBSTACLE",
            "HIGH",
            "REPORTED",
            "SIMULATED",
            "Bloqueio de teste",
            now,
            now.plusHours(2));
    var saved = call("/api/occurrences", HttpMethod.POST, o, Occurrence.class).getBody();
    assertNotNull(saved);
    assertEquals(
        "NO_ROUTE",
        http.postForObject(
                "/api/routes", new RouteBO.Request(1L, 3L, true, false), RouteBO.Result.class)
            .status());
    assertTrue(http.getForObject("/api/alerts", String.class).contains("Bloqueio de teste"));
    var closed =
        new Occurrence(
            null,
            3L,
            "OBSTACLE",
            "HIGH",
            "CLOSED",
            "SIMULATED",
            "Bloqueio de teste",
            now,
            now.plusHours(2));
    assertEquals(
        200,
        call("/api/occurrences/" + saved.id(), HttpMethod.PUT, closed, Occurrence.class)
            .getStatusCode()
            .value());
    assertEquals(
        "FOUND",
        http.postForObject(
                "/api/routes", new RouteBO.Request(1L, 3L, true, false), RouteBO.Result.class)
            .status());
    assertFalse(http.getForObject("/api/alerts", String.class).contains("Bloqueio de teste"));
    assertEquals(
        204,
        call("/api/occurrences/" + saved.id(), HttpMethod.DELETE, null, Void.class)
            .getStatusCode()
            .value());
  }

  @Test
  void readingCreatesAlert() {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    assertEquals(
        400,
        call(
                "/api/readings",
                HttpMethod.POST,
                new Reading(null, 2L, 5, now.plusDays(1)),
                String.class)
            .getStatusCode()
            .value());
    var response =
        call("/api/readings", HttpMethod.POST, new Reading(null, 2L, 40, now), Reading.class);
    assertEquals(201, response.getStatusCode().value());
    var rows = http.getForObject("/api/occurrences", Occurrence[].class);
    var created =
        Arrays.stream(rows)
            .filter(o -> o.id() >= 1000 && o.segmentId() == 2L && o.type().equals("FLOOD"))
            .findFirst()
            .orElseThrow();
    assertTrue(http.getForObject("/api/alerts", String.class).contains("40 cm"));
    call("/api/occurrences/" + created.id(), HttpMethod.DELETE, null, Void.class);
  }

  @Test
  void expiredOccurrenceIgnoredWithWarning() {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var saved =
        call(
                "/api/occurrences",
                HttpMethod.POST,
                new Occurrence(
                    null,
                    3L,
                    "FLOOD",
                    "HIGH",
                    "REPORTED",
                    "SIMULATED",
                    "Vencido",
                    now.minusHours(2),
                    now.minusHours(1)),
                Occurrence.class)
            .getBody();
    try {
      var route =
          http.postForObject(
              "/api/routes", new RouteBO.Request(1L, 3L, true, false), RouteBO.Result.class);
      assertEquals("FOUND", route.status());
      assertTrue(route.warnings().stream().anyMatch(w -> w.contains("vencida")));
    } finally {
      call("/api/occurrences/" + saved.id(), HttpMethod.DELETE, null, Void.class);
    }
  }
}
