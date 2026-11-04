package br.com.saferoute;
import jakarta.validation.constraints.*;
public record Region(Long id, @NotBlank @Size(max=100) String name, @NotBlank @Size(max=100) String city) {}
