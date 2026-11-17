package com.sachin.aiassistant.service;

import com.sachin.aiassistant.entity.DocumentChunk;
import com.sachin.aiassistant.entity.KnowledgeDocument;
import com.sachin.aiassistant.entity.Organization;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DecisionEngineTest {
    private final DecisionEngine engine = new DecisionEngine();

    @Test void evaluatesMultipleScholarshipConditionsAsStructuredDecisions() {
        Organization org = new Organization("Demo College");
        KnowledgeDocument doc = new KnowledgeDocument(org,"Scholarship_Rules.pdf","sample.pdf","/tmp/sample.pdf",100,1,1);
        String policy = "Merit scholarship eligibility: Minimum GPA of 8.0. Students must be in second year or above. " +
                "Students receiving another scholarship are not eligible for the merit scholarship.";
        DocumentChunk chunk = new DocumentChunk(org,doc,2,policy);
        QuestionAnalysis analysis = new QuestionAnalysis("DECISION","MERIT",2,8.5,true);

        var decisions = engine.evaluate(analysis,"Am I eligible for merit scholarship?",List.of(new RetrievalService.RetrievedPassage(chunk,20)));

        assertEquals(3, decisions.size());
        assertEquals("MERIT", decisions.get(0).outcome());
        assertEquals("NOT_ELIGIBLE", decisions.get(1).outcome());
        assertTrue(decisions.get(2).outcome().contains("/100"));
    }

    @Test void refusesToGuessWhenPolicyDoesNotContainEligibilityRules() {
        Organization org = new Organization("Demo College");
        KnowledgeDocument doc = new KnowledgeDocument(org,"Overview.pdf","sample.pdf","/tmp/sample.pdf",100,1,1);
        DocumentChunk chunk = new DocumentChunk(org,doc,1,"The college has a library and a student support office.");
        var decisions = engine.evaluate(new QuestionAnalysis("DECISION","GENERAL",null,null,null),"Am I eligible?",List.of(new RetrievalService.RetrievedPassage(chunk,3)));
        assertEquals("INSUFFICIENT_POLICY", decisions.get(1).outcome());
    }
}
