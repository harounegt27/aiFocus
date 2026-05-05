package com.example.aiFocus.service;

import com.example.aiFocus.dto.ManualEventRequest;
import com.example.aiFocus.dto.FreeSlot;
import com.example.aiFocus.entity.*;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.CalendarEventRepository;
import com.example.aiFocus.security.GoogleTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CalendarService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private GoogleTokenStore googleTokenStore;

    @InjectMocks
    private CalendarService calendarService;

    private User testUser;
    private CalendarEvent testEvent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        testEvent = CalendarEvent.builder()
                .id(1L)
                .user(testUser)
                .title("Test Meeting")
                .startTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)))
                .endTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0)))
                .type(EventType.MEETING)
                .source(EventSource.GOOGLE)
                .build();
    }

    /**
     * Test: getTodayEvents should return only today's events.
     */
    @Test
    void testGetTodayEvents_returnsOnlyTodayEvents() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

        List<CalendarEvent> expectedEvents = List.of(testEvent);

        when(calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                testUser, startOfDay, endOfDay))
                .thenReturn(expectedEvents);

        List<CalendarEvent> result = calendarService.getTodayEvents(testUser);

        assertEquals(1, result.size());
        assertEquals(testEvent.getTitle(), result.get(0).getTitle());
        verify(calendarEventRepository, times(1))
                .findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                        eq(testUser), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    /**
     * Test: addManualEvent should save with source MANUAL.
     */
    @Test
    void testAddManualEvent_savesWithSourceManual() {
        ManualEventRequest request = new ManualEventRequest();
        request.setTitle("Manual Focus Block");
        request.setStartTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)));
        request.setEndTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0)));
        request.setType(EventType.FOCUS);

        CalendarEvent savedEvent = CalendarEvent.builder()
                .id(2L)
                .user(testUser)
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .type(request.getType())
                .source(EventSource.MANUAL)
                .syncedAt(LocalDateTime.now())
                .build();

        when(calendarEventRepository.save(any(CalendarEvent.class)))
                .thenReturn(savedEvent);

        CalendarEvent result = calendarService.addManualEvent(testUser, request);

        assertEquals(EventSource.MANUAL, result.getSource());
        assertEquals(EventType.FOCUS, result.getType());
        assertEquals("Manual Focus Block", result.getTitle());
        verify(calendarEventRepository, times(1)).save(any(CalendarEvent.class));
    }

    /**
     * Test: getFreeSlots should return correct gaps between events.
     */
    @Test
    void testGetFreeSlots_returnsCorrectGaps() {
        LocalDate date = LocalDate.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);

        // Create events with gaps
        CalendarEvent event1 = CalendarEvent.builder()
                .id(1L)
                .user(testUser)
                .title("Event 1")
                .startTime(date.atTime(LocalTime.of(8, 0)))
                .endTime(date.atTime(LocalTime.of(9, 0)))
                .type(EventType.MEETING)
                .source(EventSource.MANUAL)
                .build();

        CalendarEvent event2 = CalendarEvent.builder()
                .id(2L)
                .user(testUser)
                .title("Event 2")
                .startTime(date.atTime(LocalTime.of(10, 0)))
                .endTime(date.atTime(LocalTime.of(11, 0)))
                .type(EventType.MEETING)
                .source(EventSource.MANUAL)
                .build();

        List<CalendarEvent> events = List.of(event1, event2);

        when(calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                testUser, startOfDay, endOfDay))
                .thenReturn(events);

        List<FreeSlot> freeSlots = calendarService.getFreeSlots(testUser, date);

        // Should have free slots between events
        assertTrue(freeSlots.size() > 0);
        // Check that one slot is from 9:00 to 10:00 (60 minutes)
        boolean hasOneHourSlot = freeSlots.stream()
                .anyMatch(slot -> slot.getDurationMinutes() >= 60);
        assertTrue(hasOneHourSlot);
    }

    /**
     * Test: syncGoogleCalendar should throw if no token found.
     */
    @Test
    void testSyncGoogleCalendar_throwsIfNoTokenFound() {
        when(googleTokenStore.getToken(testUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> calendarService.syncGoogleCalendar(testUser));

        verify(googleTokenStore, times(1)).getToken(testUser.getId());
    }

    /**
     * Test: syncGoogleCalendar should call GoogleCalendarService when token exists.
     */
    @Test
    void testSyncGoogleCalendar_callsGoogleServiceWhenTokenExists() {
        String testToken = "test-token";
        List<CalendarEvent> syncedEvents = List.of(testEvent);

        when(googleTokenStore.getToken(testUser.getId()))
                .thenReturn(Optional.of(testToken));

        when(googleCalendarService.fetchEventsFromGoogle(
                eq(testUser), eq(testToken),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(syncedEvents);

        List<CalendarEvent> result = calendarService.syncGoogleCalendar(testUser);

        assertEquals(1, result.size());
        verify(googleTokenStore, times(1)).getToken(testUser.getId());
        verify(googleCalendarService, times(1)).fetchEventsFromGoogle(
                eq(testUser), eq(testToken),
                any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Test: getEventsInRange should return events within date range.
     */
    @Test
    void testGetEventsInRange_returnsEventsInRange() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(2);
        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<CalendarEvent> expectedEvents = List.of(testEvent);

        when(calendarEventRepository.findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                testUser, startDateTime, endDateTime))
                .thenReturn(expectedEvents);

        List<CalendarEvent> result = calendarService.getEventsInRange(testUser, from, to);

        assertEquals(1, result.size());
        verify(calendarEventRepository, times(1))
                .findByUserAndStartTimeBetweenOrderByStartTimeAsc(
                        eq(testUser), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}

