package br.com.saferoute;

import jakarta.validation.constraints.*;

public record Point(
    Long id,
    @NotNull @Positive Long regionId,
    @NotBlank @Size(max = 100) String name,
    @NotNull @Min(0) @Max(1000) Integer x,
    @NotNull @Min(0) @Max(1000) Integer y) {}
