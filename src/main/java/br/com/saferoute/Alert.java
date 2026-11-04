package br.com.saferoute;
import jakarta.validation.constraints.*;
public record Alert(Long id, @NotNull @Positive Long occurrenceId, @NotBlank @Size(max=500) String message, @NotNull java.time.LocalDateTime createdAt) {}
