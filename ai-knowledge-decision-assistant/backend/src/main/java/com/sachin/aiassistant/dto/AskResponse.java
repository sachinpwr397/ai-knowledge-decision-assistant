package com.sachin.aiassistant.dto;
import java.util.List;
public record AskResponse(String answer, List<SourceReference> sources, List<DecisionResult> decisions,
                          boolean answered, String processingMode) {}
