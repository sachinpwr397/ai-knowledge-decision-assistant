package com.sachin.aiassistant.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_chunk_document", columnList = "document_id"),
        @Index(name = "idx_chunk_org", columnList = "organization_id")
})
public class DocumentChunk {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false) private KnowledgeDocument document;
    @Column(name = "page_number", nullable = false) private int pageNumber;
    @Column(nullable = false, length = 5000) private String content;
    protected DocumentChunk() {}
    public DocumentChunk(Organization organization, KnowledgeDocument document, int pageNumber, String content) {
        this.organization = organization; this.document = document; this.pageNumber = pageNumber; this.content = content;
    }
    public UUID getId() { return id; } public Organization getOrganization() { return organization; }
    public KnowledgeDocument getDocument() { return document; } public int getPageNumber() { return pageNumber; } public String getContent() { return content; }
}
