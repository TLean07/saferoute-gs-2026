package br.com.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
public class PointController {
  private final PointDAO dao;
  private final PointBO bo;

  public PointController(PointDAO dao, PointBO bo) {
    this.dao = dao;
    this.bo = bo;
  }

  @GetMapping
  public List<Point> list() {
    return dao.list();
  }

  @GetMapping("/{id}")
  public Point get(@PathVariable long id) {
    return dao.get(id);
  }

  @PostMapping
  public ResponseEntity<Point> create(@Valid @RequestBody Point v) {
    var saved = bo.create(v);
    return ResponseEntity.created(URI.create("/api/points/" + saved.id())).body(saved);
  }

  @PutMapping("/{id}")
  public Point update(@PathVariable long id, @Valid @RequestBody Point v) {
    return bo.update(id, v);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    bo.delete(id);
  }
}
