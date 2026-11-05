package com.tlean07.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {
  private final SensorDAO dao;
  private final SensorBO bo;

  public SensorController(SensorDAO dao, SensorBO bo) {
    this.dao = dao;
    this.bo = bo;
  }

  @GetMapping
  public List<Sensor> list() {
    return dao.list();
  }

  @GetMapping("/{id}")
  public Sensor get(@PathVariable long id) {
    return dao.get(id);
  }

  @PostMapping
  public ResponseEntity<Sensor> create(@Valid @RequestBody Sensor v) {
    var saved = bo.create(v);
    return ResponseEntity.created(URI.create("/api/sensors/" + saved.id())).body(saved);
  }

  @PutMapping("/{id}")
  public Sensor update(@PathVariable long id, @Valid @RequestBody Sensor v) {
    return bo.update(id, v);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    bo.delete(id);
  }
}
