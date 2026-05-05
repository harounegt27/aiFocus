package com.example.aiFocus.repository;

import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.EventSource;
import com.example.aiFocus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CalendarEvent entity.
 * Provides database operations for calendar event management.
 */
@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Finds all events for a user ordered by start time in ascending order.
     *
     * @param user the user to find events for
     * @return list of calendar events ordered by start time
     */
    List<CalendarEvent> findByUserOrderByStartTimeAsc(User user);

    /**
     * Finds all events for a user within a date range ordered by start time.
     *
     * @param user  the user to find events for
     * @param start the start datetime (inclusive)
     * @param end   the end datetime (inclusive)
     * @return list of calendar events in the date range ordered by start time
     */
    List<CalendarEvent> findByUserAndStartTimeBetweenOrderByStartTimeAsc(
            User user, LocalDateTime start, LocalDateTime end);

    /**
     * Finds all events for a user from a specific source ordered by start time.
     *
     * @param user   the user to find events for
     * @param source the event source (GOOGLE or MANUAL)
     * @return list of calendar events from the specified source
     */
    List<CalendarEvent> findByUserAndSourceOrderByStartTimeAsc(User user, EventSource source);

    /**
     * Checks if an event already exists with the same user, title, and start time.
     * Used to avoid duplicate events from Google Calendar syncs.
     *
     * @param user      the user
     * @param title     the event title
     * @param startTime the event start time
     * @return true if an event with these parameters exists
     */
    boolean existsByUserAndTitleAndStartTime(User user, String title, LocalDateTime startTime);
}

