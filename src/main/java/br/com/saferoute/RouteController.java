package br.com.saferoute;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/routes") public class RouteController {private final RouteBO bo;public RouteController(RouteBO bo){this.bo=bo;}@PostMapping public RouteBO.Result calculate(@RequestBody RouteBO.Request r){return bo.calculate(r);}}
