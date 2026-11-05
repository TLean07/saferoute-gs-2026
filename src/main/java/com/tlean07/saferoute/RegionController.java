package com.tlean07.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/regions")
public class RegionController {
  private final RegionDAO dao;
  private final RegionBO bo;

  public RegionController(RegionDAO dao, RegionBO bo) {
    this.dao = dao;
    this.bo = bo;
  }

  @GetMapping
  public List<Region> list() {
    return dao.list();
  }

  @GetMapping("/{id}")
  public Region get(@PathVariable long id) {
    return dao.get(id);
  }

  @PostMapping
  public ResponseEntity<Region> create(@Valid @RequestBody Region v) {
    var saved = bo.create(v);
    return ResponseEntity.created(URI.create("/api/regions/" + saved.id())).body(saved);
  }

  @PutMapping("/{id}")
  public Region update(@PathVariable long id, @Valid @RequestBody Region v) {
    return bo.update(id, v);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    bo.delete(id);
  }
}
