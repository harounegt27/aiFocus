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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service for user registration and login.
 * Handles user creation, password hashing, and JWT token generation.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user.
     * Validates that email is not already in use, hashes password, and saves user.
     * Returns JWT token for immediate authentication.
     *
     * @param request registration request with user details
     * @return AuthResponse with JWT token and expiration
     * @throws ConflictException if email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        // Create new user with hashed password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .timezone(request.getTimezone())
                .build();

        userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(user);
        long expiresIn = 86400000; // 24 hours in milliseconds

        return new AuthResponse(token, expiresIn);
    }

    /**
     * Authenticates a user and returns JWT token.
     * Validates user credentials and generates token if valid.
     *
     * @param request login request with email and password
     * @return AuthResponse with JWT token and expiration
     * @throws ResourceNotFoundException if user not found
     * @throws BadCredentialsException if password is incorrect
     */
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Authenticate with Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Generate JWT token
        String token = jwtService.generateToken(user);
        long expiresIn = 86400000; // 24 hours in milliseconds

        return new AuthResponse(token, expiresIn);
    }
}

