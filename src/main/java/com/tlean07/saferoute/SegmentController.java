package com.tlean07.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/segments")
public class SegmentController {
  private final SegmentDAO dao;
  private final SegmentBO bo;

  public SegmentController(SegmentDAO dao, SegmentBO bo) {
    this.dao = dao;
    this.bo = bo;
  }

  @GetMapping
  public List<Segment> list() {
    return dao.list();
  }

  @GetMapping("/{id}")
  public Segment get(@PathVariable long id) {
    return dao.get(id);
  }

  @PostMapping
  public ResponseEntity<Segment> create(@Valid @RequestBody Segment v) {
    var saved = bo.create(v);
    return ResponseEntity.created(URI.create("/api/segments/" + saved.id())).body(saved);
  }

  @PutMapping("/{id}")
  public Segment update(@PathVariable long id, @Valid @RequestBody Segment v) {
    return bo.update(id, v);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    bo.delete(id);
  }
}
