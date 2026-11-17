package com.sachin.aiassistant.repository;
import com.sachin.aiassistant.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    @Query("select c from DocumentChunk c join fetch c.document d where c.organization.id = :organizationId")
    List<DocumentChunk> findAllForOrganization(@Param("organizationId") UUID organizationId);
    @Modifying @Query("delete from DocumentChunk c where c.document.id = :documentId and c.organization.id = :organizationId")
    void deleteForDocument(@Param("documentId") UUID documentId, @Param("organizationId") UUID organizationId);
}
