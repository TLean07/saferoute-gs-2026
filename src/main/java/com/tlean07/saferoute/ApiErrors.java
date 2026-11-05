package com.tlean07.saferoute;

import java.time.*;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
  private ResponseEntity<?> error(int status, String msg) {
    return ResponseEntity.status(status)
        .body(Map.of("status", status, "message", msg, "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(Missing.class)
  public ResponseEntity<?> missing(Missing e) {
    return error(404, e.getMessage());
  }

  @ExceptionHandler(Conflict.class)
  public ResponseEntity<?> conflict(Conflict e) {
    return error(409, e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> integrity(Exception e) {
    return error(409, "Operação viola integridade ou registro possui dependências");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return error(
        400,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Entrada inválida"));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<?> input(Exception e) {
    return error(
        400,
        e instanceof IllegalArgumentException ? e.getMessage() : "Formato de requisição inválido");
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<?> database(Exception e) {
    return error(503, "Banco de dados temporariamente indisponível");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> other(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Falha interna", e);
    return error(500, "Erro interno; operação não confirmada");
  }
}
