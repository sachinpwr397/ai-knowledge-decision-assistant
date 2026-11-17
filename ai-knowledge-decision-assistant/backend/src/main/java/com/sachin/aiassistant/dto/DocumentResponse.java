package com.sachin.aiassistant.dto;
import java.time.Instant;
import java.util.UUID;
public record DocumentResponse(UUID id, String filename, long sizeBytes, int pages, int chunks, Instant uploadedAt) {}
