package com.example.aiFocus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for calendar event responses.
 * Contains the essential information about a calendar event.
 */
@Data
@AllArgsConstructor
public class CalendarEventResponse {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String type;
    private String source;
}

