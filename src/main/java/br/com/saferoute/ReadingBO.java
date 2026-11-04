package br.com.saferoute;

import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingBO {
  private final SensorDAO sensors;
  private final ReadingDAO readings;
  private final OccurrenceDAO occurrences;
  private final OccurrenceBO bo;

  public ReadingBO(SensorDAO s, ReadingDAO r, OccurrenceDAO o, OccurrenceBO b) {
    sensors = s;
    readings = r;
    occurrences = o;
    bo = b;
  }

  @Transactional
  public Reading create(Reading r) {
    var s = sensors.get(r.sensorId());
    if (s.active() != 1) throw new Conflict("Sensor inativo");
    var now = LocalDateTime.now(ZoneOffset.UTC);
    if (r.observedAt().isAfter(now.plusMinutes(5)) || r.observedAt().isBefore(now.minusHours(24)))
      throw new IllegalArgumentException(
          "Leitura deve estar nas últimas 24 horas, tolerância futura de 5 minutos");
    var saved = readings.create(r);
    boolean active =
        occurrences.list().stream()
            .anyMatch(
                o ->
                    o.segmentId().equals(s.segmentId())
                        && o.type().equals("FLOOD")
                        && !o.status().equals("CLOSED")
                        && o.validUntil().isAfter(now));
    if (r.levelCm() >= s.thresholdCm() && !active && r.observedAt().isAfter(now.minusHours(1)))
      bo.create(
          new Occurrence(
              null,
              s.segmentId(),
              "FLOOD",
              "HIGH",
              "REPORTED",
              "SIMULATED",
              "Sensor simulado acima do limiar didático: " + r.levelCm() + " cm",
              r.observedAt(),
              r.observedAt().plusHours(1)));
    return saved;
  }
}
