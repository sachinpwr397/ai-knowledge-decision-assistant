package com.sachin.aiassistant.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AskRequest(@NotBlank String assistantToken, @NotBlank @Size(max=2000) String question) {}
