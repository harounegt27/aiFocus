package com.example.aiFocus.service;

import com.example.aiFocus.dto.AuthResponse;
import com.example.aiFocus.dto.LoginRequest;
import com.example.aiFocus.dto.RegisterRequest;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.exception.BadCredentialsException;
import com.example.aiFocus.exception.ConflictException;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.UserRepository;
import com.example.aiFocus.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests user registration, login, and authentication flows.
 */
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder(10);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    @DisplayName("Should register user and return token")
    void testRegister_savesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setTimezone("UTC");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("test-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals(86400000L, response.getExpiresIn());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException if email already exists")
    void testRegister_throwsConflictException_ifEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Duplicate User");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        User existingUser = User.builder()
                .id(1L)
                .email("existing@example.com")
                .name("Existing User")
                .passwordHash("hash")
                .build();

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login with valid credentials and return token")
    void testLogin_withValidCredentials_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        String hashedPassword = passwordEncoder.encode("password123");
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .passwordHash(hashedPassword)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtService.generateToken(any(User.class))).thenReturn("login-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("login-token", response.getToken());
        assertEquals(86400000L, response.getExpiresIn());
        verify(userRepository, times(1)).findByEmail("user@example.com");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException if password is wrong")
    void testLogin_withWrongPassword_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongPassword");

        String hashedPassword = passwordEncoder.encode("correctPassword");
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .passwordHash(hashedPassword)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if user not found")
    void testLogin_withUnknownEmail_throwsResourceNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }
}

