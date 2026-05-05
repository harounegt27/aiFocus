package com.example.aiFocus.controller;

import com.example.aiFocus.dto.ManualEventRequest;
import com.example.aiFocus.entity.*;
import com.example.aiFocus.repository.CalendarEventRepository;
import com.example.aiFocus.repository.UserRepository;
import com.example.aiFocus.security.GoogleTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CalendarController.
 * Tests calendar endpoints with real database (H2 in-memory).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=YourSuperSecretKeyThatIsAtLeast32CharactersLong!",
        "app.jwt.expiration=86400000",
        "google.client.id=test-client-id",
        "google.client.secret=test-client-secret",
        "google.redirect.uri=http://localhost:8081/api/calendar/oauth2/callback",
        "google.calendar.scopes=https://www.googleapis.com/auth/calendar.readonly"
})
@DisplayName("CalendarController Integration Tests")
@Transactional
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private GoogleTokenStore googleTokenStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clear repositories
        calendarEventRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .name("Calendar Test User")
                .email("calendar@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .timezone("UTC")
                .build();
        testUser = userRepository.save(testUser);

        // Login to get token
        String loginJson = objectMapper.writeValueAsString(
                new LoginRequest("calendar@example.com", "password123")
        );

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        jwtToken = jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("GET /today should require authentication")
    void testGetToday_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/calendar/today"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /today should return 200 with valid token")
    void testGetToday_withValidToken_returns200() throws Exception {
        mockMvc.perform(get("/api/calendar/today")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.ArrayList.class)));
    }

    @Test
    @DisplayName("POST /events/manual should create event with valid data")
    void testAddManualEvent_withValidBody_returns200() throws Exception {
        ManualEventRequest request = new ManualEventRequest();
        request.setTitle("Focus Block");
        request.setStartTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)));
        request.setEndTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0)));
        request.setType(EventType.FOCUS);

        mockMvc.perform(post("/api/calendar/events/manual")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Focus Block"))
                .andExpect(jsonPath("$.type").value("FOCUS"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    @DisplayName("POST /events/manual should return 400 with missing title")
    void testAddManualEvent_withMissingTitle_returns400() throws Exception {
        ManualEventRequest request = new ManualEventRequest();
        request.setTitle("");
        request.setStartTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)));
        request.setEndTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0)));
        request.setType(EventType.FOCUS);

        mockMvc.perform(post("/api/calendar/events/manual")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /free-slots should return free time slots")
    void testGetFreeSlots_returnsSlotList() throws Exception {
        // Create an event to create a gap
        CalendarEvent event = CalendarEvent.builder()
                .user(testUser)
                .title("Meeting")
                .startTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)))
                .endTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0)))
                .type(EventType.MEETING)
                .source(EventSource.MANUAL)
                .build();
        calendarEventRepository.save(event);

        mockMvc.perform(get("/api/calendar/free-slots")
                .header("Authorization", "Bearer " + jwtToken)
                .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.ArrayList.class)))
                .andExpect(jsonPath("$[*].startTime").isArray());
    }

    @Test
    @DisplayName("GET /events?from=...&to=... should return events in range")
    void testGetEventsInRange_returnsEventsInDateRange() throws Exception {
        // Create test events
        LocalDate today = LocalDate.now();
        CalendarEvent event = CalendarEvent.builder()
                .user(testUser)
                .title("Test Event")
                .startTime(today.atTime(LocalTime.of(10, 0)))
                .endTime(today.atTime(LocalTime.of(11, 0)))
                .type(EventType.MEETING)
                .source(EventSource.MANUAL)
                .build();
        calendarEventRepository.save(event);

        mockMvc.perform(get("/api/calendar/events")
                .header("Authorization", "Bearer " + jwtToken)
                .param("from", today.toString())
                .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    @DisplayName("GET /connect should return authorization URL")
    void testConnectGoogleCalendar_returnsAuthUrl() throws Exception {
        mockMvc.perform(get("/api/calendar/connect")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUrl").isString())
                .andExpect(jsonPath("$.authUrl", startsWith("https://accounts.google.com/")));
    }

    @Test
    @DisplayName("GET /oauth2/callback should permit without token")
    void testOauthCallback_permitsWithoutToken() throws Exception {
        // This endpoint should handle the callback without JWT
        // It will return a redirect, so we expect status 302
        mockMvc.perform(get("/api/calendar/oauth2/callback")
                .param("code", "test-code"))
                .andExpect(status().isFound()); // 302 redirect
    }

    @Test
    @DisplayName("GET /sync should require authentication")
    void testSyncGoogleCalendar_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/calendar/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /sync should throw if no Google token connected")
    void testSyncGoogleCalendar_throwsIfNotConnected() throws Exception {
        mockMvc.perform(get("/api/calendar/sync")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound()); // No token connected
    }

    /**
     * Simple LoginRequest DTO for testing.
     */
    static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}

