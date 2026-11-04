package br.com.saferoute;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
@Repository
public class SegmentDAO {
 private final NamedParameterJdbcTemplate db;
 public SegmentDAO(NamedParameterJdbcTemplate db) { this.db=db; }
 public List<Segment> list() { return db.query("SELECT * FROM SR_SEGMENT ORDER BY ID",new DataClassRowMapper<>(Segment.class)); }
 public Segment get(long id) { return db.query("SELECT * FROM SR_SEGMENT WHERE ID=:id",Map.of("id",id),new DataClassRowMapper<>(Segment.class)).stream().findFirst().orElseThrow(()->new Missing("Segment não encontrado")); }
 public Segment create(Segment v) {
  long id=db.getJdbcTemplate().queryForObject("SELECT SR_SEGMENT_SEQ.NEXTVAL FROM DUAL",Long.class);
  db.update("INSERT INTO SR_SEGMENT (ID,FROM_ID,TO_ID,NAME,METERS,ACCESSIBLE,LIGHTING,BLOCKED) VALUES (:id,:fromId, :toId, :name, :meters, :accessible, :lighting, :blocked)",parameters(v).addValue("id",id));return get(id);
 }
 public Segment update(long id,Segment v) { get(id);db.update("UPDATE SR_SEGMENT SET FROM_ID=:fromId,TO_ID=:toId,NAME=:name,METERS=:meters,ACCESSIBLE=:accessible,LIGHTING=:lighting,BLOCKED=:blocked WHERE ID=:id",parameters(v).addValue("id",id));return get(id); }
 public void delete(long id) { get(id);db.update("DELETE FROM SR_SEGMENT WHERE ID=:id",Map.of("id",id)); }
 private MapSqlParameterSource parameters(Segment v) { var p=new MapSqlParameterSource();p.addValue("fromId", v.fromId());
p.addValue("toId", v.toId());
p.addValue("name", v.name());
p.addValue("meters", v.meters());
p.addValue("accessible", v.accessible());
p.addValue("lighting", v.lighting());
p.addValue("blocked", v.blocked());return p; }
}
