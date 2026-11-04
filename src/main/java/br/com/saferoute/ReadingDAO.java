package br.com.saferoute;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
@Repository
public class ReadingDAO {
 private final NamedParameterJdbcTemplate db;
 public ReadingDAO(NamedParameterJdbcTemplate db) { this.db=db; }
 public List<Reading> list() { return db.query("SELECT * FROM SR_READING ORDER BY ID",new DataClassRowMapper<>(Reading.class)); }
 public Reading get(long id) { return db.query("SELECT * FROM SR_READING WHERE ID=:id",Map.of("id",id),new DataClassRowMapper<>(Reading.class)).stream().findFirst().orElseThrow(()->new Missing("Reading não encontrado")); }
 public Reading create(Reading v) {
  long id=db.getJdbcTemplate().queryForObject("SELECT SR_READING_SEQ.NEXTVAL FROM DUAL",Long.class);
  db.update("INSERT INTO SR_READING (ID,SENSOR_ID,LEVEL_CM,OBSERVED_AT) VALUES (:id,:sensorId, :levelCm, :observedAt)",parameters(v).addValue("id",id));return get(id);
 }
 public Reading update(long id,Reading v) { get(id);db.update("UPDATE SR_READING SET SENSOR_ID=:sensorId,LEVEL_CM=:levelCm,OBSERVED_AT=:observedAt WHERE ID=:id",parameters(v).addValue("id",id));return get(id); }
 public void delete(long id) { get(id);db.update("DELETE FROM SR_READING WHERE ID=:id",Map.of("id",id)); }
 private MapSqlParameterSource parameters(Reading v) { var p=new MapSqlParameterSource();p.addValue("sensorId", v.sensorId());
p.addValue("levelCm", v.levelCm());
p.addValue("observedAt", v.observedAt());return p; }
}
