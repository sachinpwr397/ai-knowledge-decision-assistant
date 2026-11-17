package com.sachin.aiassistant.service;

import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.entity.Organization;
import com.sachin.aiassistant.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class QuestionProcessingService {
    private final OrganizationRepository organizations;
    private final RetrievalService retrieval;
    private final OpenAiService openAi;
    private final DecisionEngine decisionEngine;

    public QuestionProcessingService(OrganizationRepository organizations, RetrievalService retrieval,
                                     OpenAiService openAi, DecisionEngine decisionEngine) {
        this.organizations=organizations; this.retrieval=retrieval; this.openAi=openAi; this.decisionEngine=decisionEngine;
    }

    @Transactional
    public AskResponse ask(AskRequest request) {
        Organization organization=organizations.findByAssistantToken(request.assistantToken().trim())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"This assistant link is invalid or has been regenerated."));
        String question=request.question().trim();
        List<RetrievalService.RetrievedPassage> passages=retrieval.retrieve(organization.getId(),question);
        if(passages.isEmpty()){
            organization.recordQuestion(false);
            return new AskResponse("I couldn't find relevant information for this question in this organization's knowledge base. Please try rephrasing the question or contact the organization for help.",List.of(),List.of(),false,"GROUNDED_RETRIEVAL");
        }

        String evidence=formatEvidence(passages);
        QuestionAnalysis analysis=openAi.analyzeQuestion(question,evidence);
        List<DecisionResult> decisions=decisionEngine.evaluate(analysis,question,passages);
        Optional<String> generated=openAi.generateAnswer(question,evidence,decisions);
        String answer=generated.orElseGet(()->fallbackAnswer(passages,decisions));
        organization.recordQuestion(true);
        Map<String,SourceReference> uniqueSources=new LinkedHashMap<>();
        for(var p:passages){
            SourceReference source=new SourceReference(p.chunk().getDocument().getOriginalFilename(),p.chunk().getPageNumber(),excerpt(p.chunk().getContent()));
            uniqueSources.putIfAbsent(source.document()+"#"+source.page(),source);
        }
        List<SourceReference> sources=new ArrayList<>(uniqueSources.values());
        return new AskResponse(answer,sources,decisions,true,
                openAi.isConfigured()?"OPENAI+SYSTEM1":"KEYWORD_RETRIEVAL+SYSTEM1_FALLBACK");
    }

    private String formatEvidence(List<RetrievalService.RetrievedPassage> passages){
        StringBuilder b=new StringBuilder();
        for(var p:passages){b.append("[Source: ").append(p.chunk().getDocument().getOriginalFilename())
                .append(" — Page ").append(p.chunk().getPageNumber()).append("]\n")
                .append(p.chunk().getContent()).append("\n\n");}
        return b.toString();
    }
    private String fallbackAnswer(List<RetrievalService.RetrievedPassage> passages,List<DecisionResult> decisions){
        StringBuilder b=new StringBuilder();
        if(!decisions.isEmpty()){
            b.append("Structured assessment based on the retrieved policy excerpts:\n");
            for(DecisionResult d:decisions)b.append("• ").append(d.type()).append(": ").append(d.outcome()).append(" — ").append(d.rationale()).append("\n");
            b.append("\nPlease review the cited policy pages before treating this as a final institutional decision.");
        }else{
            b.append("I found the following relevant information in this organization's documents:\n\n");
            passages.stream().limit(3).forEach(p->b.append("• ").append(p.chunk().getContent(),0,Math.min(420,p.chunk().getContent().length())).append("\n\n"));
        }
        return b.toString();
    }
    private String excerpt(String content){String clean=content.replaceAll("\\s+"," ").trim();return clean.substring(0,Math.min(220,clean.length()));}
}
