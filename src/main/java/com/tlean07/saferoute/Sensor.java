package com.tlean07.saferoute;

import jakarta.validation.constraints.*;

public record Sensor(
    Long id,
    @NotNull @Positive Long segmentId,
    @NotBlank @Size(max = 100) String name,
    @NotNull @Min(1) @Max(1000) Integer thresholdCm,
    @NotNull @Min(0) @Max(1) Integer active) {}
