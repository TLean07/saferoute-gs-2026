package br.com.saferoute;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
@Repository
public class AlertDAO {
 private final NamedParameterJdbcTemplate db;
 public AlertDAO(NamedParameterJdbcTemplate db) { this.db=db; }
 public List<Alert> list() { return db.query("SELECT * FROM SR_ALERT ORDER BY ID",new DataClassRowMapper<>(Alert.class)); }
 public Alert get(long id) { return db.query("SELECT * FROM SR_ALERT WHERE ID=:id",Map.of("id",id),new DataClassRowMapper<>(Alert.class)).stream().findFirst().orElseThrow(()->new Missing("Alert não encontrado")); }
 public Alert create(Alert v) {
  long id=db.getJdbcTemplate().queryForObject("SELECT SR_ALERT_SEQ.NEXTVAL FROM DUAL",Long.class);
  db.update("INSERT INTO SR_ALERT (ID,OCCURRENCE_ID,MESSAGE,CREATED_AT) VALUES (:id,:occurrenceId, :message, :createdAt)",parameters(v).addValue("id",id));return get(id);
 }
 public Alert update(long id,Alert v) { get(id);db.update("UPDATE SR_ALERT SET OCCURRENCE_ID=:occurrenceId,MESSAGE=:message,CREATED_AT=:createdAt WHERE ID=:id",parameters(v).addValue("id",id));return get(id); }
 public void delete(long id) { get(id);db.update("DELETE FROM SR_ALERT WHERE ID=:id",Map.of("id",id)); }
 private MapSqlParameterSource parameters(Alert v) { var p=new MapSqlParameterSource();p.addValue("occurrenceId", v.occurrenceId());
p.addValue("message", v.message());
p.addValue("createdAt", v.createdAt());return p; }
}
