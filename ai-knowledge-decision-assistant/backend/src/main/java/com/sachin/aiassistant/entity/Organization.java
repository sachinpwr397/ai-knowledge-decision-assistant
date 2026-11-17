package com.sachin.aiassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "assistant_token", nullable = false, unique = true, length = 36) private String assistantToken;
    @Column(name = "questions_asked", nullable = false) private long questionsAsked = 0;
    @Column(name = "questions_answered", nullable = false) private long questionsAnswered = 0;
    @Column(name = "questions_unanswered", nullable = false) private long questionsUnanswered = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected Organization() {}
    public Organization(String name) { this.name = name; this.assistantToken = UUID.randomUUID().toString(); }
    @PrePersist void onCreate() { createdAt = Instant.now(); if (assistantToken == null) assistantToken = UUID.randomUUID().toString(); }
    public void rotateAssistantToken() { assistantToken = UUID.randomUUID().toString(); }
    public void recordQuestion(boolean answered) { questionsAsked++; if (answered) questionsAnswered++; else questionsUnanswered++; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAssistantToken() { return assistantToken; }
    public long getQuestionsAsked() { return questionsAsked; }
    public long getQuestionsAnswered() { return questionsAnswered; }
    public long getQuestionsUnanswered() { return questionsUnanswered; }
    public Instant getCreatedAt() { return createdAt; }
}
