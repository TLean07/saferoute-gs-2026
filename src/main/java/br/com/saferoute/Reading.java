package br.com.saferoute;

import jakarta.validation.constraints.*;

public record Reading(
    Long id,
    @NotNull @Positive Long sensorId,
    @NotNull @Min(0) @Max(10000) Integer levelCm,
    @NotNull java.time.LocalDateTime observedAt) {}
