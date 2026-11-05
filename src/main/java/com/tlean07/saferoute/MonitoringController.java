package com.tlean07.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MonitoringController {
  private final ReadingDAO readings;
  private final ReadingBO bo;
  private final AlertDAO alerts;
  private final OccurrenceDAO occurrences;
  private final PointDAO points;
  private final SegmentDAO segments;
  private final JdbcTemplate jdbc;
  private final RealNetwork network;

  public MonitoringController(
      ReadingDAO r,
      ReadingBO b,
      AlertDAO a,
      OccurrenceDAO o,
      PointDAO p,
      SegmentDAO s,
      JdbcTemplate j,
      RealNetwork network) {
    readings = r;
    bo = b;
    alerts = a;
    occurrences = o;
    points = p;
    segments = s;
    jdbc = j;
    this.network = network;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    jdbc.queryForObject("SELECT 1 FROM DUAL", Integer.class);
    return Map.of("status", "UP", "service", "SafeRoute");
  }

  @GetMapping("/readings")
  public List<Reading> readings() {
    return readings.list();
  }

  @PostMapping("/readings")
  public ResponseEntity<Reading> add(@Valid @RequestBody Reading v) {
    var r = bo.create(v);
    return ResponseEntity.created(URI.create("/api/readings/" + r.id())).body(r);
  }

  @GetMapping("/alerts")
  public List<Map<String, Object>> alerts() {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var current = new HashMap<Long, Occurrence>();
    for (var o : network.occurrences(occurrences.list()))
      if (!o.status().equals("CLOSED") && o.validUntil().isAfter(now)) current.put(o.id(), o);
    return alerts.list().stream()
        .filter(a -> current.containsKey(a.occurrenceId()))
        .map(
            a ->
                Map.<String, Object>of(
                    "id",
                    a.id(),
                    "createdAt",
                    a.createdAt(),
                    "occurrence",
                    current.get(a.occurrenceId()),
                    "message",
                    "Verifique a ocorrência atual: " + current.get(a.occurrenceId()).description()))
        .toList();
  }

  @GetMapping("/graph")
  public Map<String, Object> graph() {
    return network.graph(points.list(), segments.list(), occurrences.list());
  }
}
