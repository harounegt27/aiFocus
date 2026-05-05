package com.example.aiFocus.service;

import com.example.aiFocus.dto.CalendarEventResponse;
import com.example.aiFocus.dto.FreeSlot;
import com.example.aiFocus.dto.ManualEventRequest;
import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.EventSource;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.CalendarEventRepository;
import com.example.aiFocus.security.GoogleTokenStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing calendar events.
 * Handles calendar operations including syncing, creating events, and computing free slots.
 */
@Service
@Slf4j
@Transactional
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final GoogleCalendarService googleCalendarService;
    private final GoogleTokenStore googleTokenStore;

    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(20, 0);
    private static final int MIN_SLOT_MINUTES = 30;

    public CalendarService(
            CalendarEventRepository calendarEventRepository,
            GoogleCalendarService googleCalendarService,
            GoogleTokenStore googleTokenStore) {
        this.calendarEventRepository = calendarEventRepository;
        this.googleCalendarService = googleCalendarService;
        this.googleTokenStore = googleTokenStore;
    }

    /**
     * Syncs events from Google Calendar for the given user.
     * Retrieves events for the next 7 days starting from today.
     *
     * @param user the user to sync for
     * @return list of newly synced calendar events
     * @throws ResourceNotFoundException if no Google token is found for the user
     */
    public List<CalendarEvent> syncGoogleCalendar(User user) {
        String accessToken = googleTokenStore.getToken(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Google Calendar not connected for this user. Please connect first."));

        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        List<CalendarEvent> syncedEvents = googleCalendarService.fetchEventsFromGoogle(
                user, accessToken, today, weekFromNow);

        log.info("Synced {} events for user {}", syncedEvents.size(), user.getId());
        return syncedEvents;
    }

    /**
     * Creates a manual calendar event for the given user.
     *
     * @param user    the user
     * @param request the manual event request
     * @return the saved calendar event
     */
    public CalendarEvent addManualEvent(User user, ManualEventRequest request) {
        CalendarEvent event = CalendarEvent.builder()
                .user(user)
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .type(request.getType())
                .source(EventSource.MANUAL)
                .syncedAt(LocalDateTime.now())
                .build();

        CalendarEvent saved = calendarEventRepository.save(event);
        log.info("Created manual event: {} for user {}", request.getTitle(), user.getId());
        return saved;
    }

    /**
     * Retrieves all events for today (00:00 to 23:59).
     *
     * @param user the user
     * @return list of calendar events for today
     */
    public List<CalendarEvent> getTodayEvents(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

        return calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                user, startOfDay, endOfDay);
    }

    /**
     * Retrieves all events for a user within a date range.
     *
     * @param user the user
     * @param from the start date (inclusive)
     * @param to   the end date (inclusive)
     * @return list of calendar events in the range
     */
    public List<CalendarEvent> getEventsInRange(User user, LocalDate from, LocalDate to) {
        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay().minusSeconds(1);

        return calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                user, startDateTime, endDateTime);
    }

    /**
     * Computes free time slots for a given date.
     * Free slots are calculated between 08:00 and 20:00, only considering slots >= 30 minutes.
     *
     * @param user the user
     * @param date the date to compute free slots for
     * @return list of free slots
     */
    public List<FreeSlot> getFreeSlots(User user, LocalDate date) {
        List<FreeSlot> freeSlots = new ArrayList<>();

        // Get all events for the day
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);

        List<CalendarEvent> events = calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                user, startOfDay, endOfDay);

        // Sort events by start time
        List<CalendarEvent> sortedEvents = events.stream()
                .sorted((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
                .collect(Collectors.toList());

        // Calculate free slots
        LocalDateTime workStart = date.atTime(WORK_START);
        LocalDateTime workEnd = date.atTime(WORK_END);

        if (sortedEvents.isEmpty()) {
            // Entire work day is free
            long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                    .between(workStart, workEnd);
            freeSlots.add(new FreeSlot(workStart, workEnd, (int) durationMinutes));
        } else {
            // First slot: from work start to first event
            LocalDateTime firstEventStart = sortedEvents.get(0).getStartTime();
            if (workStart.isBefore(firstEventStart)) {
                long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                        .between(workStart, firstEventStart);
                if (durationMinutes >= MIN_SLOT_MINUTES) {
                    freeSlots.add(new FreeSlot(workStart, firstEventStart, (int) durationMinutes));
                }
            }

            // Middle slots: between events
            for (int i = 0; i < sortedEvents.size() - 1; i++) {
                LocalDateTime currentEventEnd = sortedEvents.get(i).getEndTime();
                LocalDateTime nextEventStart = sortedEvents.get(i + 1).getStartTime();

                long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                        .between(currentEventEnd, nextEventStart);

                if (durationMinutes >= MIN_SLOT_MINUTES) {
                    freeSlots.add(new FreeSlot(currentEventEnd, nextEventStart, (int) durationMinutes));
                }
            }

            // Last slot: from last event to work end
            LocalDateTime lastEventEnd = sortedEvents.get(sortedEvents.size() - 1).getEndTime();
            if (lastEventEnd.isBefore(workEnd)) {
                long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                        .between(lastEventEnd, workEnd);
                if (durationMinutes >= MIN_SLOT_MINUTES) {
                    freeSlots.add(new FreeSlot(lastEventEnd, workEnd, (int) durationMinutes));
                }
            }
        }

        log.info("Computed {} free slots for user {} on {}", freeSlots.size(), user.getId(), date);
        return freeSlots;
    }

    /**
     * Converts a CalendarEvent to CalendarEventResponse DTO.
     *
     * @param event the calendar event
     * @return the response DTO
     */
    public CalendarEventResponse toResponse(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getEndTime(),
                event.getType().toString(),
                event.getSource().toString()
        );
    }
}

