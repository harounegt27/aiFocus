package com.example.aiFocus.service;

import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.EventSource;
import com.example.aiFocus.entity.EventType;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.repository.CalendarEventRepository;
import com.example.aiFocus.security.GoogleTokenStore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for Google Calendar API integration.
 * Handles OAuth authorization and event synchronization.
 */
@Service
@Slf4j
public class GoogleCalendarService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Value("${google.calendar.scopes}")
    private String scopes;

    private final CalendarEventRepository calendarEventRepository;
    private final GoogleTokenStore googleTokenStore;

    private static final String APPLICATION_NAME = "Focus Time Optimizer";

    public GoogleCalendarService(
            CalendarEventRepository calendarEventRepository,
            GoogleTokenStore googleTokenStore) {
        this.calendarEventRepository = calendarEventRepository;
        this.googleTokenStore = googleTokenStore;
    }

    /**
     * Builds the Google OAuth authorization URL for the user.
     *
     * @param userId the user ID
     * @return the authorization URL
     */
    public String buildAuthorizationUrl(Long userId) {
        try {
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    jsonFactory,
                    clientId,
                    clientSecret,
                    Arrays.asList(scopes.split(","))
            )
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

            return flow.newAuthorizationUrl().setRedirectUri(redirectUri)
                        .setState(userId.toString())
                    .build();
        } catch (Exception e) {
            log.error("Error building authorization URL", e);
            throw new RuntimeException("Failed to build authorization URL");
        }
    }

    /**
     * Exchanges the OAuth authorization code for an access token.
     *
     * @param code the authorization code from Google
     * @return the access token
     */
    public String exchangeCodeForToken(String code) {
        try {
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    jsonFactory,
                    clientId,
                    clientSecret,
                    Arrays.asList(scopes.split(","))
            )
                    .setAccessType("offline")
                    .build();

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            String accessToken = tokenResponse.getAccessToken();
            log.info("Successfully obtained Google access token");
            return accessToken;
        } catch (IOException e) {
            log.error("Error exchanging authorization code for token", e);
            throw new RuntimeException("Failed to exchange authorization code");
        }
    }

    /**
     * Fetches events from Google Calendar for the specified date range.
     * Only creates new events if they don't already exist in the database.
     *
     * @param user        the user
     * @param accessToken the Google access token
     * @param from        the start date (inclusive)
     * @param to          the end date (inclusive)
     * @return list of newly synced calendar events
     */
    public List<CalendarEvent> fetchEventsFromGoogle(
            User user, String accessToken,
            LocalDate from, LocalDate to) {
        List<CalendarEvent> newEvents = new ArrayList<>();

        try {
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleCredential credential = new GoogleCredential()
                    .setAccessToken(accessToken);

            Calendar service = new Calendar.Builder(
                    new NetHttpTransport(),
                    jsonFactory,
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

            Events events = service.events().list("primary")
                    .setTimeMin(convertToGoogleDateTime(fromDateTime))
                    .setTimeMax(convertToGoogleDateTime(toDateTime))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            for (Event googleEvent : events.getItems()) {
                LocalDateTime eventStartTime = convertFromGoogleDateTime(googleEvent.getStart());
                LocalDateTime eventEndTime = convertFromGoogleDateTime(googleEvent.getEnd());

                // Skip all-day events or cancelled events
                if (eventStartTime == null || eventEndTime == null) {
                    continue;
                }

                // Check if event already exists
                String title = googleEvent.getSummary();
                if (calendarEventRepository.existsByUserAndTitleAndStartTime(
                        user, title, eventStartTime)) {
                    log.debug("Event already exists: {} at {}", title, eventStartTime);
                    continue;
                }

                CalendarEvent calendarEvent = CalendarEvent.builder()
                        .user(user)
                        .title(title)
                        .startTime(eventStartTime)
                        .endTime(eventEndTime)
                        .type(EventType.MEETING)
                        .source(EventSource.GOOGLE)
                        .syncedAt(LocalDateTime.now())
                        .build();

                CalendarEvent saved = calendarEventRepository.save(calendarEvent);
                newEvents.add(saved);
                log.debug("Synced Google event: {}", title);
            }

            log.info("Successfully synced {} events from Google Calendar for user {}", 
                    newEvents.size(), user.getId());

        } catch (IOException e) {
            log.error("Error fetching events from Google Calendar", e);
            throw new RuntimeException("Failed to fetch events from Google Calendar");
        }

        return newEvents;
    }

    /**
     * Converts LocalDateTime to Google DateTime format (RFC 3339).
     */
    private com.google.api.client.util.DateTime convertToGoogleDateTime(LocalDateTime dateTime) {
        return new com.google.api.client.util.DateTime(
                java.time.ZonedDateTime.of(
                        dateTime,
                        ZoneId.systemDefault()
                ).toInstant().toEpochMilli()
        );
    }

    /**
     * Converts Google EventDateTime to LocalDateTime.
     */
    private LocalDateTime convertFromGoogleDateTime(com.google.api.services.calendar.model.EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }
        
        // EventDateTime can have either dateTime or date
        if (eventDateTime.getDateTime() != null) {
            // It has a full datetime
            com.google.api.client.util.DateTime googleDateTime = eventDateTime.getDateTime();
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleDateTime.getValue()),
                    ZoneId.systemDefault()
            );
        }
        
        // Skip all-day events (they only have a date, not datetime)
        return null;
    }
}






