package com.example.aiFocus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing a free time slot for the user.
 */
@Data
@AllArgsConstructor
public class FreeSlot {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;
}

