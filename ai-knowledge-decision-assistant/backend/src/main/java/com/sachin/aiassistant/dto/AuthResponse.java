package com.sachin.aiassistant.dto;
import java.util.UUID;
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                           UUID userId, String fullName, String email, String role,
                           UUID organizationId, String organizationName, String assistantToken) {}
