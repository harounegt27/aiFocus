package com.example.aiFocus.controller;

import com.example.aiFocus.dto.CalendarEventResponse;
import com.example.aiFocus.dto.FreeSlot;
import com.example.aiFocus.dto.ManualEventRequest;
import com.example.aiFocus.dto.SyncResponse;
import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.service.CalendarService;
import com.example.aiFocus.service.GoogleCalendarService;
import com.example.aiFocus.security.GoogleTokenStore;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for calendar operations.
 * All endpoints require JWT authentication except /oauth2/callback.
 */
@RestController
@RequestMapping("/api/calendar")
@Slf4j
public class CalendarController {

    private final CalendarService calendarService;
    private final GoogleCalendarService googleCalendarService;
    private final GoogleTokenStore googleTokenStore;

    public CalendarController(
            CalendarService calendarService,
            GoogleCalendarService googleCalendarService,
            GoogleTokenStore googleTokenStore) {
        this.calendarService = calendarService;
        this.googleCalendarService = googleCalendarService;
        this.googleTokenStore = googleTokenStore;
    }

    /**
     * GET /api/calendar/connect
     * Returns the Google OAuth authorization URL for connecting the user's Google Calendar.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @return response containing the authorization URL
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connectGoogleCalendar(
            @AuthenticationPrincipal User currentUser) {
        log.info("User {} requesting Google Calendar connection URL", currentUser.getId());

        String authUrl = googleCalendarService.buildAuthorizationUrl(currentUser.getId());

        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/calendar/oauth2/callback?code=...
     * OAuth2 callback endpoint - exchanges authorization code for access token.
     * Does NOT require JWT authentication (permitAll in SecurityConfig).
     * Redirects to /api/calendar/sync after successful token exchange.
     *
     * @param code the authorization code from Google
     * @return redirect to sync endpoint
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<Map<String, String>> oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {  // ← add state parameter
        log.info("Received OAuth2 callback with authorization code");

        try {
            Long userId = Long.parseLong(state);  // ← extract userId
            String accessToken = googleCalendarService.exchangeCodeForToken(code);
            googleTokenStore.saveToken(userId, accessToken);  // ← store for correct user
            log.info("Successfully stored Google token for userId: {}", userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Google Calendar connected successfully!");
            response.put("userId", userId.toString());
            response.put("next", "Call GET /api/calendar/sync with your JWT token in Postman");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing OAuth2 callback", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to connect Google Calendar");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/calendar/sync
     * Syncs events from Google Calendar for the authenticated user.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @return SyncResponse containing count of synced events and event list
     */
    @GetMapping("/sync")
    public ResponseEntity<SyncResponse> syncGoogleCalendar(
            @AuthenticationPrincipal User currentUser) {
        log.info("User {} requesting Google Calendar sync", currentUser.getId());

        List<CalendarEvent> syncedEvents = calendarService.syncGoogleCalendar(currentUser);

        List<CalendarEventResponse> eventResponses = syncedEvents.stream()
                .map(calendarService::toResponse)
                .collect(Collectors.toList());

        SyncResponse response = new SyncResponse(syncedEvents.size(), eventResponses);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/calendar/today
     * Retrieves all calendar events for today.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @return list of calendar events for today
     */
    @GetMapping("/today")
    public ResponseEntity<List<CalendarEventResponse>> getTodayEvents(
            @AuthenticationPrincipal User currentUser) {
        log.info("User {} requesting today's events", currentUser.getId());

        List<CalendarEvent> events = calendarService.getTodayEvents(currentUser);
        List<CalendarEventResponse> responses = events.stream()
                .map(calendarService::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/calendar/events?from=yyyy-MM-dd&to=yyyy-MM-dd
     * Retrieves all calendar events within the specified date range.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @param from        the start date (inclusive)
     * @param to          the end date (inclusive)
     * @return list of calendar events in the range
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getEventsInRange(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("User {} requesting events from {} to {}", currentUser.getId(), from, to);

        List<CalendarEvent> events = calendarService.getEventsInRange(currentUser, from, to);
        List<CalendarEventResponse> responses = events.stream()
                .map(calendarService::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/calendar/free-slots?date=yyyy-MM-dd
     * Computes and returns free time slots for a given date.
     * Free slots are between 08:00 and 20:00 and must be at least 30 minutes long.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @param date        the date to compute free slots for
     * @return list of free time slots
     */
    @GetMapping("/free-slots")
    public ResponseEntity<List<FreeSlot>> getFreeSlotsForDate(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("User {} requesting free slots for {}", currentUser.getId(), date);

        List<FreeSlot> freeSlots = calendarService.getFreeSlots(currentUser, date);
        return ResponseEntity.ok(freeSlots);
    }

    /**
     * POST /api/calendar/events/manual
     * Creates a manual calendar event for the authenticated user.
     * Requires JWT authentication.
     *
     * @param currentUser the authenticated user
     * @param request     the manual event creation request
     * @return the created calendar event response
     */
    @PostMapping("/events/manual")
    public ResponseEntity<CalendarEventResponse> createManualEvent(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid ManualEventRequest request) {
        log.info("User {} creating manual event: {}", currentUser.getId(), request.getTitle());

        CalendarEvent event = calendarService.addManualEvent(currentUser, request);
        CalendarEventResponse response = calendarService.toResponse(event);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

