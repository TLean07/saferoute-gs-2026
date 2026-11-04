package br.com.saferoute;

import java.util.*;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class SensorDAO {
  private final NamedParameterJdbcTemplate db;

  public SensorDAO(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  public List<Sensor> list() {
    return db.query("SELECT * FROM SR_SENSOR ORDER BY ID", new DataClassRowMapper<>(Sensor.class));
  }

  public Sensor get(long id) {
    return db
        .query(
            "SELECT * FROM SR_SENSOR WHERE ID=:id",
            Map.of("id", id),
            new DataClassRowMapper<>(Sensor.class))
        .stream()
        .findFirst()
        .orElseThrow(() -> new Missing("Sensor não encontrado"));
  }

  public Sensor create(Sensor v) {
    long id =
        db.getJdbcTemplate().queryForObject("SELECT SR_SENSOR_SEQ.NEXTVAL FROM DUAL", Long.class);
    db.update(
        "INSERT INTO SR_SENSOR (ID,SEGMENT_ID,NAME,THRESHOLD_CM,ACTIVE) VALUES (:id,:segmentId,"
            + " :name, :thresholdCm, :active)",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public Sensor update(long id, Sensor v) {
    get(id);
    db.update(
        "UPDATE SR_SENSOR SET"
            + " SEGMENT_ID=:segmentId,NAME=:name,THRESHOLD_CM=:thresholdCm,ACTIVE=:active WHERE"
            + " ID=:id",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public void delete(long id) {
    get(id);
    db.update("DELETE FROM SR_SENSOR WHERE ID=:id", Map.of("id", id));
  }

  private MapSqlParameterSource parameters(Sensor v) {
    var p = new MapSqlParameterSource();
    p.addValue("segmentId", v.segmentId());
    p.addValue("name", v.name());
    p.addValue("thresholdCm", v.thresholdCm());
    p.addValue("active", v.active());
    return p;
  }
}
