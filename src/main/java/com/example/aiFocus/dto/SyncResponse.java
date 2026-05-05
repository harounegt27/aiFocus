package com.example.aiFocus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * DTO for Google Calendar sync response.
 */
@Data
@AllArgsConstructor
public class SyncResponse {
    private int syncedCount;
    private List<CalendarEventResponse> events;
}

