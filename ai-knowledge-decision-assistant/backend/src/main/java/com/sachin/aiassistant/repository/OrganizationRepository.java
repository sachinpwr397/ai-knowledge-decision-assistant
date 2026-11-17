package com.sachin.aiassistant.repository;
import com.sachin.aiassistant.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByAssistantToken(String assistantToken);
}
