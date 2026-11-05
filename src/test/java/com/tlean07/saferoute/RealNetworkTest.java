package com.tlean07.saferoute;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "saferoute.real-network=true",
      "spring.datasource.url=jdbc:h2:mem:realnetwork;MODE=Oracle;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("demo")
class RealNetworkTest {
  @Autowired RealNetwork network;
  @Autowired PointDAO points;
  @Autowired SegmentDAO segments;
  @Autowired OccurrenceBO occurrences;
  @Autowired RouteBO routes;
  @Autowired JdbcTemplate db;
  @Autowired TestRestTemplate http;

  @Test
  void importIsIdempotentAndUsesGeographicData() {
    var ps = network.points(points.list());
    assertTrue(ps.size() > 100);
    assertEquals(900L, ps.get(0).regionId());
    int count = db.queryForObject("SELECT COUNT(*) FROM SR_POINT", Integer.class);
    network.afterPropertiesSet();
    assertEquals(count, db.queryForObject("SELECT COUNT(*) FROM SR_POINT", Integer.class));
    var graph = http.getForObject("/api/graph", Map.class);
    assertEquals(false, graph.get("simulated"));
    assertTrue(((List<?>) graph.get("occurrences")).isEmpty());
    assertTrue(((Map<?, ?>) ((List<?>) graph.get("points")).get(0)).containsKey("latitude"));
  }

  @Test
  void realRouteHonorsIncidentAndDoesNotAssumeAccessibility() {
    var edge = network.segments(segments.list()).get(0);
    var request = new RouteBO.Request(edge.fromId(), edge.toId(), false, false);
    assertEquals("FOUND", routes.calculate(request).status());
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var incident =
        occurrences.create(
            new Occurrence(
                null,
                edge.id(),
                "FLOOD",
                "HIGH",
                "REPORTED",
                "OPERATOR",
                "Ocorrência em teste automatizado isolado",
                now,
                now.plusHours(1)));
    try {
      assertFalse(routes.calculate(request).segments().contains(edge.id()));
    } finally {
      occurrences.delete(incident.id());
    }
    assertEquals(
        "NO_ROUTE",
        routes.calculate(new RouteBO.Request(edge.fromId(), edge.toId(), true, false)).status());
  }

  @Test
  void simulatedIncidentRejectedOnRealStreet() {
    var edge = network.segments(segments.list()).get(0);
    var now = LocalDateTime.now(ZoneOffset.UTC);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            occurrences.create(
                new Occurrence(
                    null,
                    edge.id(),
                    "FLOOD",
                    "HIGH",
                    "REPORTED",
                    "SIMULATED",
                    "Teste",
                    now,
                    now.plusHours(1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> routes.calculate(new RouteBO.Request(1L, 3L, false, false)));
  }
}
