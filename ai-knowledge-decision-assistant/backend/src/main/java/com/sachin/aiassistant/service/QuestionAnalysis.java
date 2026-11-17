package com.sachin.aiassistant.service;

import java.util.List;

public record QuestionAnalysis(String intent, String policyCategory, Integer academicYear,
                               Double gpa, Boolean hasOtherScholarship, List<String> decisionChecks) {
    public QuestionAnalysis(String intent, String policyCategory, Integer academicYear,
                            Double gpa, Boolean hasOtherScholarship) {
        this(intent, policyCategory, academicYear, gpa, hasOtherScholarship, List.of());
    }
    public QuestionAnalysis {
        decisionChecks = decisionChecks == null ? List.of() : List.copyOf(decisionChecks);
    }
    public boolean requiresDecision() { return "DECISION".equalsIgnoreCase(intent); }
}
