package com.tlean07.saferoute;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

class WeatherServiceTest {
  HttpServer server;
  AtomicInteger hits = new AtomicInteger();
  int code = 200;
  String body =
      "{\"current\":{\"time\":\"2026-11-05T00:30\",\"temperature_2m\":23.3,\"precipitation\":0},\"hourly\":{\"time\":[]}}";

  static class TestClock extends Clock {
    Instant time = Instant.parse("2026-11-05T00:35:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return time;
    }
  }

  TestClock clock = new TestClock();
  WeatherService service;

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        e -> {
          hits.incrementAndGet();
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          e.sendResponseHeaders(code, bytes.length);
          e.getResponseBody().write(bytes);
          e.close();
        });
    server.start();
    service =
        new WeatherService(
            new ObjectMapper(),
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
            clock);
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void freshDataUsesCache() {
    assertEquals("CURRENT", service.current().get("status"));
    assertEquals("CURRENT", service.current().get("status"));
    assertEquals(1, hits.get());
  }

  @Test
  void unavailableDoesNotInventZeroRain() {
    code = 429;
    var r = service.current();
    assertEquals("UNAVAILABLE", r.get("status"));
    assertNull(r.get("data"));
    service.current();
    assertEquals(1, hits.get());
  }

  @Test
  void staleCacheExpires() {
    service.current();
    clock.time = clock.time.plusSeconds(901);
    code = 503;
    assertEquals("STALE", service.current().get("status"));
    clock.time = clock.time.plusSeconds(7200);
    var r = service.current();
    assertEquals("UNAVAILABLE", r.get("status"));
    assertNull(r.get("data"));
  }

  @Test
  void invalidOrOldPayloadRejected() {
    body = "{}";
    assertEquals("UNAVAILABLE", service.current().get("status"));
    clock.time = clock.time.plusSeconds(61);
    body =
        "{\"current\":{\"time\":\"2020-01-01T00:00\",\"temperature_2m\":20,\"precipitation\":0},\"hourly\":{\"time\":[]}}";
    assertEquals("UNAVAILABLE", service.current().get("status"));
  }
}
