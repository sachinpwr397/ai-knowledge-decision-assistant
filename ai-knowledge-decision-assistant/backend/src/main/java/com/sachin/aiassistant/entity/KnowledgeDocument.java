package com.sachin.aiassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents", indexes = @Index(name = "idx_document_org", columnList = "organization_id"))
public class KnowledgeDocument {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
    @Column(name = "stored_filename", nullable = false, length = 80) private String storedFilename;
    @Column(name = "storage_path", nullable = false, length = 700) private String storagePath;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "page_count", nullable = false) private int pageCount;
    @Column(name = "chunk_count", nullable = false) private int chunkCount;
    @Column(name = "uploaded_at", nullable = false, updatable = false) private Instant uploadedAt;
    protected KnowledgeDocument() {}
    public KnowledgeDocument(Organization organization, String originalFilename, String storedFilename, String storagePath,
                             long fileSize, int pageCount, int chunkCount) {
        this.organization = organization; this.originalFilename = originalFilename; this.storedFilename = storedFilename;
        this.storagePath = storagePath; this.fileSize = fileSize; this.pageCount = pageCount; this.chunkCount = chunkCount;
    }
    @PrePersist void onCreate() { uploadedAt = Instant.now(); }
    public UUID getId() { return id; } public Organization getOrganization() { return organization; }
    public String getOriginalFilename() { return originalFilename; } public String getStoredFilename() { return storedFilename; }
    public String getStoragePath() { return storagePath; } public long getFileSize() { return fileSize; }
    public int getPageCount() { return pageCount; } public int getChunkCount() { return chunkCount; } public Instant getUploadedAt() { return uploadedAt; }
}
