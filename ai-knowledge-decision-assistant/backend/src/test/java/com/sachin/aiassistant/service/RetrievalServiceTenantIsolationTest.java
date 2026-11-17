package com.sachin.aiassistant.service;

import com.sachin.aiassistant.entity.DocumentChunk;
import com.sachin.aiassistant.entity.KnowledgeDocument;
import com.sachin.aiassistant.entity.Organization;
import com.sachin.aiassistant.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetrievalServiceTenantIsolationTest {
    @Test void retrievalLoadsChunksOnlyForRequestedOrganization() {
        DocumentChunkRepository repository=mock(DocumentChunkRepository.class);
        Organization tenantA=new Organization("Tenant A");
        KnowledgeDocument documentA=new KnowledgeDocument(tenantA,"A.pdf","a.pdf","/tmp/a.pdf",100,1,1);
        DocumentChunk chunkA=new DocumentChunk(tenantA,documentA,1,"The library opens at eight in the morning.");
        UUID tenantAId=UUID.randomUUID();
        when(repository.findAllForOrganization(tenantAId)).thenReturn(List.of(chunkA));
        RetrievalService service=new RetrievalService(repository,6,1);

        var results=service.retrieve(tenantAId,"When does the library open?");

        assertEquals(1,results.size());
        assertSame(chunkA,results.get(0).chunk());
        verify(repository).findAllForOrganization(tenantAId);
        verifyNoMoreInteractions(repository);
    }
}
