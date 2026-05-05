package com.example.aiFocus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CalendarEvent entity representing a calendar event in the Focus Time Optimizer.
 * Events can be synced from Google Calendar or manually created by users.
 */
@Entity
@Table(name = "calendar_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventSource source;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}

