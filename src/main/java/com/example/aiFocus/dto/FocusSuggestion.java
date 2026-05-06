package com.example.aiFocus.dto;

import java.time.LocalDateTime;

public record FocusSuggestion(
    String start,
    String end,
    double confidence,
    String reason
) {}
