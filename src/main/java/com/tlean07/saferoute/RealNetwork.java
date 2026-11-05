package com.tlean07.saferoute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Versioned OSM extract. Oracle stores the routing graph; this asset preserves geographic
 * provenance.
 */
@Component
@DependsOnDatabaseInitialization
public class RealNetwork implements InitializingBean {
  private final JdbcTemplate db;
  private final TransactionTemplate transaction;
  private final boolean enabled;
  private final JsonNode snapshot;
  private final Map<Long, JsonNode> pointMetadata = new HashMap<>();
  private final Map<Long, JsonNode> segmentMetadata = new HashMap<>();

  public RealNetwork(
      JdbcTemplate db,
      TransactionTemplate transaction,
      ObjectMapper mapper,
      @Value("${saferoute.real-network:true}") boolean enabled)
      throws IOException {
    this.db = db;
    this.transaction = transaction;
    this.enabled = enabled;
    try (var in = new ClassPathResource("osm-network.json").getInputStream()) {
      snapshot = mapper.readTree(in);
    }
    snapshot.path("points").forEach(p -> pointMetadata.put(p.path("id").asLong(), p));
    snapshot.path("segments").forEach(s -> segmentMetadata.put(s.path("id").asLong(), s));
    if (pointMetadata.isEmpty() || segmentMetadata.isEmpty())
      throw new IOException("Malha OSM vazia");
  }

  @Override
  public void afterPropertiesSet() {
    if (!enabled) return;
    transaction.executeWithoutResult(
        status -> {
          if (db.queryForObject("SELECT COUNT(*) FROM SR_REGION WHERE ID=900", Integer.class) == 0)
            db.update(
                "INSERT INTO SR_REGION (ID,NAME,CITY) VALUES (900,?,?)",
                "Jardim Pantanal e entorno - OpenStreetMap",
                "São Paulo");
          Set<Long> existingPoints =
              new HashSet<>(db.queryForList("SELECT ID FROM SR_POINT", Long.class));
          List<Object[]> points = new ArrayList<>();
          pointMetadata
              .values()
              .forEach(
                  p -> {
                    if (!existingPoints.contains(p.path("id").asLong()))
                      points.add(
                          new Object[] {
                            p.path("id").asLong(),
                            900,
                            p.path("name").asText(),
                            p.path("x").asInt(),
                            p.path("y").asInt()
                          });
                  });
          if (!points.isEmpty())
            db.batchUpdate(
                "INSERT INTO SR_POINT (ID,REGION_ID,NAME,X,Y) VALUES (?,?,?,?,?)", points);
          Set<Long> existingSegments =
              new HashSet<>(db.queryForList("SELECT ID FROM SR_SEGMENT", Long.class));
          List<Object[]> segments = new ArrayList<>();
          segmentMetadata
              .values()
              .forEach(
                  s -> {
                    if (!existingSegments.contains(s.path("id").asLong()))
                      segments.add(
                          new Object[] {
                            s.path("id").asLong(),
                            s.path("fromId").asLong(),
                            s.path("toId").asLong(),
                            s.path("name").asText(),
                            s.path("meters").asInt(),
                            s.path("accessible").asInt(),
                            s.path("lighting").asInt(),
                            0
                          });
                  });
          if (!segments.isEmpty())
            db.batchUpdate(
                "INSERT INTO SR_SEGMENT (ID,FROM_ID,TO_ID,NAME,METERS,ACCESSIBLE,LIGHTING,BLOCKED)"
                    + " VALUES (?,?,?,?,?,?,?,?)",
                segments);
        });
  }

  public boolean enabled() {
    return enabled;
  }

  public boolean isRealPoint(long id) {
    return pointMetadata.containsKey(id);
  }

  public boolean isRealSegment(long id) {
    return segmentMetadata.containsKey(id);
  }

  public JsonNode point(long id) {
    return pointMetadata.get(id);
  }

  public JsonNode segment(long id) {
    return segmentMetadata.get(id);
  }

  public JsonNode metadata() {
    var result = snapshot.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) result).remove(List.of("points", "segments"));
    return result;
  }

  public List<Point> points(List<Point> all) {
    return enabled
        ? all.stream().filter(p -> p.regionId() == 900 && isRealPoint(p.id())).toList()
        : all;
  }

  public List<Segment> segments(List<Segment> all) {
    return enabled
        ? all.stream()
            .filter(s -> isRealSegment(s.id()) && isRealPoint(s.fromId()) && isRealPoint(s.toId()))
            .toList()
        : all;
  }

  public List<Occurrence> occurrences(List<Occurrence> all) {
    return enabled
        ? all.stream()
            .filter(o -> isRealSegment(o.segmentId()) && !o.source().equals("SIMULATED"))
            .toList()
        : all;
  }

  public Map<String, Object> graph(
      List<Point> points, List<Segment> segments, List<Occurrence> occurrences) {
    var ps =
        points(points).stream()
            .map(
                p -> {
                  var row = new LinkedHashMap<String, Object>();
                  row.put("id", p.id());
                  row.put("regionId", p.regionId());
                  row.put("name", p.name());
                  row.put("x", p.x());
                  row.put("y", p.y());
                  if (enabled) {
                    row.put("latitude", point(p.id()).path("latitude").asDouble());
                    row.put("longitude", point(p.id()).path("longitude").asDouble());
                  }
                  return row;
                })
            .toList();
    var ss =
        segments(segments).stream()
            .map(
                s -> {
                  var row = new LinkedHashMap<String, Object>();
                  row.put("id", s.id());
                  row.put("fromId", s.fromId());
                  row.put("toId", s.toId());
                  row.put("name", s.name());
                  row.put("meters", s.meters());
                  row.put("accessible", s.accessible());
                  row.put("lighting", s.lighting());
                  row.put("blocked", s.blocked());
                  if (enabled) {
                    row.put("osmWayId", segment(s.id()).path("osmWayId").asLong());
                    row.put(
                        "accessibility",
                        s.accessible() == 1
                            ? "yes"
                            : segment(s.id()).path("accessibility").asText());
                    row.put("lightingSource", segment(s.id()).path("lightingSource").asText());
                  }
                  return row;
                })
            .toList();
    return Map.of(
        "version",
        "2",
        "city",
        "São Paulo",
        "simulated",
        !enabled,
        "source",
        enabled ? metadata() : Map.of("area", "Rede de testes"),
        "points",
        ps,
        "segments",
        ss,
        "occurrences",
        occurrences(occurrences),
        "generatedAt",
        java.time.Instant.now());
  }
}
