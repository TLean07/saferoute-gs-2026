package br.com.saferoute;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/occurrences")
public class OccurrenceController {
  private final OccurrenceDAO dao;
  private final OccurrenceBO bo;

  public OccurrenceController(OccurrenceDAO d, OccurrenceBO b) {
    dao = d;
    bo = b;
  }

  @GetMapping
  public List<Occurrence> list() {
    return dao.list();
  }

  @GetMapping("/{id}")
  public Occurrence get(@PathVariable long id) {
    return dao.get(id);
  }

  @PostMapping
  public ResponseEntity<Occurrence> create(@Valid @RequestBody Occurrence v) {
    var s = bo.create(v);
    return ResponseEntity.created(URI.create("/api/occurrences/" + s.id())).body(s);
  }

  @PutMapping("/{id}")
  public Occurrence update(@PathVariable long id, @Valid @RequestBody Occurrence v) {
    return bo.update(id, v);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    bo.delete(id);
  }
}
