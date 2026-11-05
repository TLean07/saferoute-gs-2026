package com.tlean07.saferoute;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RouteBO {
  public record Request(Long fromId, Long toId, boolean accessible, boolean night) {}

  public record Result(
      String status,
      List<Long> points,
      List<Long> segments,
      int meters,
      double cost,
      List<String> warnings,
      LocalDateTime calculatedAt) {}

  private final PointDAO points;
  private final SegmentDAO segments;
  private final OccurrenceDAO occurrences;
  private final RealNetwork network;

  public RouteBO(PointDAO p, SegmentDAO s, OccurrenceDAO o, RealNetwork network) {
    points = p;
    segments = s;
    occurrences = o;
    this.network = network;
  }

  public Result calculate(Request r) {
    if (r.fromId() == null || r.toId() == null)
      throw new IllegalArgumentException("Origem e destino são obrigatórios");
    points.get(r.fromId());
    points.get(r.toId());
    if (network.enabled() && (!network.isRealPoint(r.fromId()) || !network.isRealPoint(r.toId())))
      throw new IllegalArgumentException("Selecione pontos da malha OpenStreetMap publicada");
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var warnings = new ArrayList<String>();
    warnings.add(
        network.enabled()
            ? "Trajeto calculado sobre ruas do OpenStreetMap. Bloqueios refletem somente"
                  + " ocorrências registradas; confira as condições locais."
            : "Rede de testes: dados simulados.");
    if (network.enabled() && r.accessible())
      warnings.add(
          "Apenas trechos com acessibilidade explicitamente cadastrada são aceitos; dados ausentes"
              + " são excluídos.");
    var forbidden = new HashSet<Long>();
    var penalties = new HashMap<Long, Double>();
    for (var o : network.occurrences(occurrences.list()))
      if (!o.status().equals("CLOSED")) {
        if (!o.validUntil().isAfter(now)) {
          warnings.add("Ocorrência " + o.id() + " vencida; condição atual desconhecida.");
          continue;
        }
        if (o.type().equals("FLOOD") || o.severity().equals("HIGH")) forbidden.add(o.segmentId());
        else penalties.merge(o.segmentId(), o.severity().equals("MEDIUM") ? .6 : .2, Math::max);
      }
    var edges = new HashMap<Long, List<Segment>>();
    for (var s : network.segments(segments.list()))
      if (s.blocked() == 0
          && !forbidden.contains(s.id())
          && (!r.accessible() || s.accessible() == 1)) {
        edges.computeIfAbsent(s.fromId(), k -> new ArrayList<>()).add(s);
        edges.computeIfAbsent(s.toId(), k -> new ArrayList<>()).add(s);
      }
    var dist = new HashMap<Long, Double>();
    var previous = new HashMap<Long, Segment>();
    record Entry(long node, double cost) {}
    var queue =
        new PriorityQueue<Entry>(
            Comparator.comparingDouble(Entry::cost).thenComparingLong(Entry::node));
    dist.put(r.fromId(), 0.0);
    queue.add(new Entry(r.fromId(), 0));
    while (!queue.isEmpty()) {
      var e = queue.poll();
      if (e.cost() > dist.getOrDefault(e.node(), Double.POSITIVE_INFINITY)) continue;
      if (e.node() == r.toId()) break;
      for (var s : edges.getOrDefault(e.node(), List.of())) {
        long next = s.fromId() == e.node() ? s.toId() : s.fromId();
        double cost =
            e.cost()
                + s.meters()
                    * (1
                        + penalties.getOrDefault(s.id(), 0.0)
                        + (r.night() && s.lighting() == 0 ? .5 : 0));
        if (cost < dist.getOrDefault(next, Double.POSITIVE_INFINITY)) {
          dist.put(next, cost);
          previous.put(next, s);
          queue.add(new Entry(next, cost));
        }
      }
    }
    if (!dist.containsKey(r.toId()))
      return new Result("NO_ROUTE", List.of(), List.of(), 0, 0, warnings, now);
    var path = new ArrayList<Long>();
    var links = new ArrayList<Long>();
    int meters = 0;
    long n = r.toId();
    path.add(n);
    while (n != r.fromId()) {
      var s = previous.get(n);
      links.add(s.id());
      meters += s.meters();
      n = s.fromId() == n ? s.toId() : s.fromId();
      path.add(n);
    }
    Collections.reverse(path);
    Collections.reverse(links);
    if (network.enabled()) {
      if (links.stream()
          .anyMatch(id -> network.segment(id).path("accessibility").asText().equals("unknown")))
        warnings.add("Há trechos sem informação de acessibilidade no OpenStreetMap.");
      if (r.night()
          && links.stream()
              .anyMatch(
                  id -> network.segment(id).path("lightingSource").asText().equals("unknown")))
        warnings.add(
            "Há trechos com iluminação não informada; o cálculo não garante iluminação no local.");
    }
    return new Result("FOUND", path, links, meters, dist.get(r.toId()), warnings, now);
  }
}
