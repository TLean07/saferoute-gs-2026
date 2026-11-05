package com.tlean07.saferoute;

import jakarta.validation.constraints.*;

public record Occurrence(
    Long id,
    @NotNull @Positive Long segmentId,
    @NotBlank @Pattern(regexp = "FLOOD|OBSTACLE|LIGHTING") String type,
    @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH") String severity,
    @NotBlank @Pattern(regexp = "REPORTED|CONFIRMED|CLOSED") String status,
    @NotBlank @Pattern(regexp = "SIMULATED|COMMUNITY|OPERATOR") String source,
    @NotBlank @Size(max = 500) String description,
    @NotNull java.time.LocalDateTime observedAt,
    @NotNull java.time.LocalDateTime validUntil) {}
