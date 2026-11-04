package br.com.saferoute;
import jakarta.validation.constraints.*;
public record Segment(Long id, @NotNull @Positive Long fromId, @NotNull @Positive Long toId, @NotBlank @Size(max=100) String name, @NotNull @Min(1) @Max(100000) Integer meters, @NotNull @Min(0) @Max(1) Integer accessible, @NotNull @Min(0) @Max(1) Integer lighting, @NotNull @Min(0) @Max(1) Integer blocked) {}
