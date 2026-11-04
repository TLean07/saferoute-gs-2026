package br.com.saferoute;

import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccurrenceBO {
  private final OccurrenceDAO dao;
  private final AlertDAO alerts;
  private final SegmentDAO segments;

  public OccurrenceBO(OccurrenceDAO d, AlertDAO a, SegmentDAO s) {
    dao = d;
    alerts = a;
    segments = s;
  }

  private void validate(Occurrence v) {
    segments.get(v.segmentId());
    if (!v.validUntil().isAfter(v.observedAt()))
      throw new IllegalArgumentException("Validade precisa ser posterior à observação");
    if (v.observedAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5)))
      throw new IllegalArgumentException("Observação futura não permitida");
  }

  @Transactional
  public Occurrence create(Occurrence v) {
    validate(v);
    var saved = dao.create(v);
    if (!saved.status().equals("CLOSED"))
      alerts.create(
          new Alert(
              null,
              saved.id(),
              "Verifique o trecho: " + saved.description(),
              LocalDateTime.now(ZoneOffset.UTC)));
    return saved;
  }

  @Transactional
  public Occurrence update(long id, Occurrence v) {
    validate(v);
    var saved = dao.update(id, v);
    if (!saved.status().equals("CLOSED")
        && alerts.list().stream().noneMatch(a -> a.occurrenceId().equals(id)))
      alerts.create(
          new Alert(
              null,
              id,
              "Verifique o trecho: " + saved.description(),
              LocalDateTime.now(ZoneOffset.UTC)));
    return saved;
  }

  @Transactional
  public void delete(long id) {
    dao.get(id);
    for (var a : alerts.list()) if (a.occurrenceId().equals(id)) alerts.delete(a.id());
    dao.delete(id);
  }
}
