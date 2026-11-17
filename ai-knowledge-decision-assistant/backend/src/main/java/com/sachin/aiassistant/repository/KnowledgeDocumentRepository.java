package com.sachin.aiassistant.repository;
import com.sachin.aiassistant.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findAllByOrganization_IdOrderByUploadedAtDesc(UUID organizationId);
    long countByOrganization_Id(UUID organizationId);
    Optional<KnowledgeDocument> findByIdAndOrganization_Id(UUID id, UUID organizationId);
}
