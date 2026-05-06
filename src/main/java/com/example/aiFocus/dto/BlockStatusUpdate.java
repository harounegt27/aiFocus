package com.example.aiFocus.dto;

import com.example.aiFocus.entity.BlockStatus;
import jakarta.validation.constraints.NotNull;

public record BlockStatusUpdate(
    @NotNull BlockStatus status
) {}
