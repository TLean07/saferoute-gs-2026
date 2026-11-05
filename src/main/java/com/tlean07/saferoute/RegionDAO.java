package com.tlean07.saferoute;

import java.util.*;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class RegionDAO {
  private final NamedParameterJdbcTemplate db;

  public RegionDAO(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  public List<Region> list() {
    return db.query("SELECT * FROM SR_REGION ORDER BY ID", new DataClassRowMapper<>(Region.class));
  }

  public Region get(long id) {
    return db
        .query(
            "SELECT * FROM SR_REGION WHERE ID=:id",
            Map.of("id", id),
            new DataClassRowMapper<>(Region.class))
        .stream()
        .findFirst()
        .orElseThrow(() -> new Missing("Region não encontrado"));
  }

  public Region create(Region v) {
    long id =
        db.getJdbcTemplate().queryForObject("SELECT SR_REGION_SEQ.NEXTVAL FROM DUAL", Long.class);
    db.update(
        "INSERT INTO SR_REGION (ID,NAME,CITY) VALUES (:id,:name, :city)",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public Region update(long id, Region v) {
    get(id);
    db.update(
        "UPDATE SR_REGION SET NAME=:name,CITY=:city WHERE ID=:id",
        parameters(v).addValue("id", id));
    return get(id);
  }

  public void delete(long id) {
    get(id);
    db.update("DELETE FROM SR_REGION WHERE ID=:id", Map.of("id", id));
  }

  private MapSqlParameterSource parameters(Region v) {
    var p = new MapSqlParameterSource();
    p.addValue("name", v.name());
    p.addValue("city", v.city());
    return p;
  }
}
