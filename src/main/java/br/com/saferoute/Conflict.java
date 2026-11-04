package br.com.saferoute;

public class Conflict extends RuntimeException {
  public Conflict(String message) {
    super(message);
  }
}
