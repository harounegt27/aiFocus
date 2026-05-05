package com.example.aiFocus.security;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory token store for Google OAuth access tokens.
 * Note: In production, tokens should be encrypted and stored in the database.
 * This implementation is suitable for the project scope.
 */
@Component
public class GoogleTokenStore {

    private final Map<Long, String> tokenCache = new HashMap<>();

    /**
     * Saves a Google OAuth access token for a user.
     *
     * @param userId      the user ID
     * @param accessToken the Google access token
     */
    public void saveToken(Long userId, String accessToken) {
        tokenCache.put(userId, accessToken);
    }

    /**
     * Retrieves the Google OAuth access token for a user.
     *
     * @param userId the user ID
     * @return Optional containing the token if found
     */
    public Optional<String> getToken(Long userId) {
        return Optional.ofNullable(tokenCache.get(userId));
    }

    /**
     * Removes the Google OAuth access token for a user.
     *
     * @param userId the user ID
     */
    public void removeToken(Long userId) {
        tokenCache.remove(userId);
    }
}

