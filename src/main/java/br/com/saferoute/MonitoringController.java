package br.com.saferoute;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import jakarta.validation.Valid;import java.util.*;import java.time.*;import java.net.URI;import org.springframework.jdbc.core.JdbcTemplate;
@RestController @RequestMapping("/api") public class MonitoringController {
 private final ReadingDAO readings;private final ReadingBO bo;private final AlertDAO alerts;private final OccurrenceDAO occurrences;private final PointDAO points;private final SegmentDAO segments;private final JdbcTemplate jdbc;
 public MonitoringController(ReadingDAO r,ReadingBO b,AlertDAO a,OccurrenceDAO o,PointDAO p,SegmentDAO s,JdbcTemplate j){readings=r;bo=b;alerts=a;occurrences=o;points=p;segments=s;jdbc=j;}
 @GetMapping("/health") public Map<String,Object> health(){jdbc.queryForObject("SELECT 1 FROM DUAL",Integer.class);return Map.of("status","UP","service","SafeRoute");}
 @GetMapping("/readings") public List<Reading> readings(){return readings.list();}
 @PostMapping("/readings") public ResponseEntity<Reading> add(@Valid @RequestBody Reading v){var r=bo.create(v);return ResponseEntity.created(URI.create("/api/readings/"+r.id())).body(r);}
 @GetMapping("/alerts") public List<Map<String,Object>> alerts(){var now=LocalDateTime.now(ZoneOffset.UTC);var current=new HashMap<Long,Occurrence>();for(var o:occurrences.list())if(!o.status().equals("CLOSED")&&o.validUntil().isAfter(now))current.put(o.id(),o);return alerts.list().stream().filter(a->current.containsKey(a.occurrenceId())).map(a->Map.<String,Object>of("id",a.id(),"createdAt",a.createdAt(),"occurrence",current.get(a.occurrenceId()),"message","Verifique a ocorrência atual: "+current.get(a.occurrenceId()).description())).toList();}
 @GetMapping("/graph") public Map<String,Object> graph(){return Map.of("version","1","city","São Paulo","simulated",true,"points",points.list(),"segments",segments.list(),"occurrences",occurrences.list(),"generatedAt",LocalDateTime.now(ZoneOffset.UTC));}
}
