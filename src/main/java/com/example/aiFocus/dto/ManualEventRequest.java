package com.example.aiFocus.dto;

import com.example.aiFocus.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for manual calendar event creation requests.
 */
@Data
public class ManualEventRequest {

    @NotBlank(message = "Event title is required")
    private String title;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Event type is required")
    private EventType type;
}

