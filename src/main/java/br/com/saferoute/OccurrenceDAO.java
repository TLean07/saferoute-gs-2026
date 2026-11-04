package br.com.saferoute;

import java.util.*;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class OccurrenceDAO {
  private final NamedParameterJdbcTemplate db;

  public OccurrenceDAO(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  public List<Occurrence> list() {
    return db.query(
        "SELECT * FROM SR_OCCURRENCE ORDER BY ID", new DataClassRowMapper<>(Occurrence.class));
  }

  public Occurrence get(long id) {
    return db
        .query(
            "SELECT * FROM SR_OCCURRENCE WHERE ID=:id",
            Map.of("id", id),
            new DataClassRowMapper<>(Occurrence.class))
        .stream()
        .findFirst()
        .orElseThrow(() -> new Missing("Occurrence não encontrado"));
  }

  public Occurrence create(Occurrence v) {
    long id =
        db.getJdbcTemplate()
            .queryForObject("SELECT SR_OCCURRENCE_SEQ.NEXTVAL FROM DUAL", Long.class);
    db.update(
        "INSERT INTO SR_OCCURRENCE"
            + " (ID,SEGMENT_ID,TYPE,SEVERITY,STATUS,SOURCE,DESCRIPTION,OBSERVED_AT,VALID_UNTIL)"
            + " VALUES (:id,:segmentId, :type, :severity, :status, :source, :description,"
            + " :observedAt, :validUntil)",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public Occurrence update(long id, Occurrence v) {
    get(id);
    db.update(
        "UPDATE SR_OCCURRENCE SET"
            + " SEGMENT_ID=:segmentId,TYPE=:type,SEVERITY=:severity,STATUS=:status,SOURCE=:source,DESCRIPTION=:description,OBSERVED_AT=:observedAt,VALID_UNTIL=:validUntil"
            + " WHERE ID=:id",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public void delete(long id) {
    get(id);
    db.update("DELETE FROM SR_OCCURRENCE WHERE ID=:id", Map.of("id", id));
  }

  private MapSqlParameterSource parameters(Occurrence v) {
    var p = new MapSqlParameterSource();
    p.addValue("segmentId", v.segmentId());
    p.addValue("type", v.type());
    p.addValue("severity", v.severity());
    p.addValue("status", v.status());
    p.addValue("source", v.source());
    p.addValue("description", v.description());
    p.addValue("observedAt", v.observedAt());
    p.addValue("validUntil", v.validUntil());
    return p;
  }
}
