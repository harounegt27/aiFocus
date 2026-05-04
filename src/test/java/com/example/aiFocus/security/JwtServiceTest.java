package com.example.aiFocus.security;

import com.example.aiFocus.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 * Tests JWT token generation, validation, and claim extraction.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "app.jwt.secret=YourSuperSecretKeyThatIsAtLeast32CharactersLong!",
        "app.jwt.expiration=86400000"
})
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .timezone("UTC")
                .build();
    }

    @Test
    @DisplayName("Should generate non-null token")
    void testGenerateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("Should extract correct email from token")
    void testExtractEmail_returnsCorrectEmail() {
        String token = jwtService.generateToken(testUser);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(testUser.getEmail(), extractedEmail);
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void testIsTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateToken(testUser);
        UserDetails userDetails = testUser;

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject token for different user")
    void testIsTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateToken(testUser);
        User differentUser = User.builder()
                .id(2L)
                .name("Different User")
                .email("different@example.com")
                .passwordHash("differentPassword")
                .build();
        UserDetails userDetails = differentUser;

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject expired token")
    void testIsTokenValid_returnsFalseForExpiredToken() {
        // Create an expired token manually
        String jwtSecret = "YourSuperSecretKeyThatIsAtLeast32CharactersLong!";
        Date expiryDate = new Date(System.currentTimeMillis() - 1000); // 1 second ago

        String expiredToken = Jwts.builder()
                .setSubject(testUser.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        boolean isValid = jwtService.isTokenValid(expiredToken, testUser);

        assertFalse(isValid);
    }
}

