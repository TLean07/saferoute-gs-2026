package br.com.saferoute;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
@Repository
public class PointDAO {
 private final NamedParameterJdbcTemplate db;
 public PointDAO(NamedParameterJdbcTemplate db) { this.db=db; }
 public List<Point> list() { return db.query("SELECT * FROM SR_POINT ORDER BY ID",new DataClassRowMapper<>(Point.class)); }
 public Point get(long id) { return db.query("SELECT * FROM SR_POINT WHERE ID=:id",Map.of("id",id),new DataClassRowMapper<>(Point.class)).stream().findFirst().orElseThrow(()->new Missing("Point não encontrado")); }
 public Point create(Point v) {
  long id=db.getJdbcTemplate().queryForObject("SELECT SR_POINT_SEQ.NEXTVAL FROM DUAL",Long.class);
  db.update("INSERT INTO SR_POINT (ID,REGION_ID,NAME,X,Y) VALUES (:id,:regionId, :name, :x, :y)",parameters(v).addValue("id",id));return get(id);
 }
 public Point update(long id,Point v) { get(id);db.update("UPDATE SR_POINT SET REGION_ID=:regionId,NAME=:name,X=:x,Y=:y WHERE ID=:id",parameters(v).addValue("id",id));return get(id); }
 public void delete(long id) { get(id);db.update("DELETE FROM SR_POINT WHERE ID=:id",Map.of("id",id)); }
 private MapSqlParameterSource parameters(Point v) { var p=new MapSqlParameterSource();p.addValue("regionId", v.regionId());
p.addValue("name", v.name());
p.addValue("x", v.x());
p.addValue("y", v.y());return p; }
}
