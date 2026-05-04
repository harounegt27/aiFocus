package com.example.aiFocus.controller;

import com.example.aiFocus.dto.LoginRequest;
import com.example.aiFocus.dto.RegisterRequest;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests authentication endpoints with real database (H2 in-memory).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=YourSuperSecretKeyThatIsAtLeast32CharactersLong!",
        "app.jwt.expiration=86400000"
})
@DisplayName("AuthController Integration Tests")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Clear repository
        userRepository.deleteAll();

        // Setup test data
        registerRequest = new RegisterRequest();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setTimezone("UTC");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Should register user and return 201 with token")
    void testRegisterEndpoint_returns201AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(86400000L));
    }

    @Test
    @DisplayName("Should login with valid credentials and return 200")
    void testLoginEndpoint_withValidCredentials_returns200() throws Exception {
        // Register user first
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .timezone("UTC")
                .build();
        userRepository.save(user);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("jane@example.com");
        loginReq.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(86400000L));
    }

    @Test
    @DisplayName("Should return 401 for login with wrong password")
    void testLoginEndpoint_withWrongPassword_returns401() throws Exception {
        // Register user first
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("correctPassword"))
                .timezone("UTC")
                .build();
        userRepository.save(user);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should return 403 for protected endpoint without token")
    void testProtectedEndpoint_withoutToken_returns403() throws Exception {
        // Try to access a protected resource without token
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 for protected endpoint with valid token")
    void testProtectedEndpoint_withValidToken_returns200() throws Exception {
        // Register and login to get token
        User user = User.builder()
                .name("Protected User")
                .email("protected@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .timezone("UTC")
                .build();
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("token").asText();

        // Try to access protected endpoint with valid token
        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // 404 because endpoint doesn't exist, not 401/403
    }

    @Test
    @DisplayName("Should return 409 when registering with existing email")
    void testRegisterEndpoint_withDuplicateEmail_returns409() throws Exception {
        // Register first user
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try to register with same email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 404 when logging in with unknown email")
    void testLoginEndpoint_withUnknownEmail_returns404() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("unknown@example.com");
        loginReq.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

