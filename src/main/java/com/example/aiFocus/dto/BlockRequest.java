package com.example.aiFocus.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record BlockRequest(
    @NotNull OffsetDateTime start,
    @NotNull OffsetDateTime end,
    @NotNull Double confidence,
    @NotNull String reason
) {}
