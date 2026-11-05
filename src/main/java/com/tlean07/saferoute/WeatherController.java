package com.tlean07.saferoute;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
  private final WeatherService service;

  public WeatherController(WeatherService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> get() {
    var data = service.current();
    return ResponseEntity.status(data.get("status").equals("UNAVAILABLE") ? 503 : 200).body(data);
  }
}
