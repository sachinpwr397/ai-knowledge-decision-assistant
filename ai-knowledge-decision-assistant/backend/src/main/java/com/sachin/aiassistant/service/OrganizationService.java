package com.sachin.aiassistant.service;
import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.entity.Organization;
import com.sachin.aiassistant.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@Service
public class OrganizationService {
    private final OrganizationRepository organizations; private final KnowledgeDocumentRepository documents;
    @Value("${app.public-base-url}") private String baseUrl;
    public OrganizationService(OrganizationRepository organizations,KnowledgeDocumentRepository documents){this.organizations=organizations;this.documents=documents;}
    @Transactional(readOnly=true) public AssistantResponse assistant(UUID organizationId){return response(find(organizationId));}
    @Transactional public AssistantResponse regenerate(UUID organizationId){Organization o=find(organizationId);o.rotateAssistantToken();return response(o);}
    @Transactional(readOnly=true) public UsageResponse usage(UUID id){Organization o=find(id);return new UsageResponse(documents.countByOrganization_Id(id),o.getQuestionsAsked(),o.getQuestionsAnswered(),o.getQuestionsUnanswered());}
    @Transactional(readOnly=true) public Organization find(UUID id){return organizations.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organization not found."));}
    private AssistantResponse response(Organization o){String url=baseUrl.replaceAll("/$","")+"/widget?token="+o.getAssistantToken();return new AssistantResponse(o.getName(),o.getAssistantToken(),url,"<iframe src=\""+url+"\" width=\"100%\" height=\"620\" style=\"border:0;border-radius:16px\" title=\"Knowwise assistant\"></iframe>");}
}
