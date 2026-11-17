package com.sachin.aiassistant.dto;
import java.util.UUID;
public record UserProfileResponse(UUID userId, String fullName, String email, String role,
                                  UUID organizationId, String organizationName) {}
