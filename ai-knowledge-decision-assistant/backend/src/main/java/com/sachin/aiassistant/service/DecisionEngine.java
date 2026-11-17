package com.sachin.aiassistant.service;

import com.sachin.aiassistant.dto.DecisionResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DecisionEngine {
    public List<DecisionResult> evaluate(QuestionAnalysis analysis, String question,
                                         List<RetrievalService.RetrievedPassage> passages) {
        if (!analysis.requiresDecision()) return List.of();
        String evidence=passages.stream().map(p->p.chunk().getContent()).collect(Collectors.joining(" "));
        CompletableFuture<DecisionResult> choice=CompletableFuture.supplyAsync(()->choiceDecision(analysis,evidence));
        CompletableFuture<DecisionResult> eligibility=CompletableFuture.supplyAsync(()->eligibilityDecision(analysis,question,evidence));
        CompletableFuture<DecisionResult> score=CompletableFuture.supplyAsync(()->scoreDecision(analysis,evidence));
        CompletableFuture.allOf(choice,eligibility,score).join();
        return List.of(choice.join(),eligibility.join(),score.join());
    }

    private DecisionResult choiceDecision(QuestionAnalysis a,String evidence){
        String category=Optional.ofNullable(a.policyCategory()).orElse("GENERAL").toUpperCase(Locale.ROOT);
        String lower=evidence.toLowerCase(Locale.ROOT);
        String keyword=switch(category){case "MERIT"->"merit";case "SPORTS"->"sport";case "NEED_BASED"->"need";default->"scholarship";};
        boolean supported=lower.contains(keyword) || (category.equals("NEED_BASED") && lower.contains("financial"));
        return new DecisionResult("CHOICE",category,supported?"The question category is present in the organization's retrieved policy text.":"The category is inferred from the question; the retrieved text does not explicitly confirm this policy category.");
    }

    private DecisionResult eligibilityDecision(QuestionAnalysis a,String question,String evidence){
        String lower=evidence.toLowerCase(Locale.ROOT);
        List<String> checks=new ArrayList<>(); boolean failed=false; boolean unknown=false;
        Double minimumGpa=findMinimumGpa(evidence);
        if(minimumGpa!=null){
            if(a.gpa()==null){unknown=true;checks.add("GPA was not provided; policy minimum is "+minimumGpa+".");}
            else if(a.gpa()>=minimumGpa)checks.add("GPA condition passed ("+a.gpa()+" ≥ "+minimumGpa+").");
            else {failed=true;checks.add("GPA condition failed ("+a.gpa()+" < "+minimumGpa+").");}
        }
        Integer minimumYear=findMinimumYear(evidence);
        if(minimumYear!=null){
            if(a.academicYear()==null){unknown=true;checks.add("Academic year was not provided; minimum year is "+minimumYear+".");}
            else if(a.academicYear()>=minimumYear)checks.add("Academic-year condition passed.");
            else {failed=true;checks.add("Academic-year condition failed.");}
        }
        boolean fullTimeRequired=lower.contains("full-time") || lower.contains("full time");
        if(fullTimeRequired){
            String q=question.toLowerCase(Locale.ROOT);
            if(q.contains("part-time") || q.contains("part time")){failed=true;checks.add("Full-time enrollment condition failed.");}
            else if(q.contains("full-time") || q.contains("full time")){checks.add("Full-time enrollment condition passed based on the user's statement.");}
            else {unknown=true;checks.add("The policy requires full-time enrollment, but the user's enrollment status was not provided.");}
        }
        boolean otherScholarshipProhibited=lower.matches("(?s).*(not eligible|ineligible|cannot|may not|must not|prohibited).{0,100}(another|other) scholarship.*")
                || lower.matches("(?s).*(another|other) scholarship.{0,100}(not eligible|ineligible|cannot|may not|must not|prohibited).*");
        if(otherScholarshipProhibited){
            if(a.hasOtherScholarship()==null){unknown=true;checks.add("The policy restricts other scholarships, but the user's existing-scholarship status is unclear.");}
            else if(a.hasOtherScholarship()){failed=true;checks.add("Existing-scholarship restriction failed.");}
            else checks.add("Existing-scholarship condition passed.");
        }
        if(checks.isEmpty()) return new DecisionResult("BOOLEAN","INSUFFICIENT_POLICY","The retrieved documents do not contain machine-readable eligibility criteria for this question.");
        String outcome=failed?"NOT_ELIGIBLE":unknown?"INSUFFICIENT_DATA":"ELIGIBLE";
        return new DecisionResult("BOOLEAN",outcome,String.join(" ",checks));
    }

    private DecisionResult scoreDecision(QuestionAnalysis a,String evidence){
        String lower=evidence.toLowerCase(Locale.ROOT);
        int score=0; int evidenceSignals=0;
        if(lower.contains("eligible") || lower.contains("eligibility")){score+=25;evidenceSignals++;}
        if(lower.contains("minimum gpa") || lower.contains("gpa of") || lower.contains("cgpa")){score+=25;evidenceSignals++;}
        if(lower.contains("year") || lower.contains("semester")){score+=20;evidenceSignals++;}
        if(lower.contains("scholarship")){score+=20;evidenceSignals++;}
        if(lower.contains("must") || lower.contains("required") || lower.contains("criteria")){score+=10;evidenceSignals++;}
        if(evidenceSignals==0) return new DecisionResult("SCORE","0/100","No explicit policy signals were found in the retrieved excerpts.");
        return new DecisionResult("SCORE",score+"/100","Evidence coverage score based on explicit policy signals in the retrieved excerpts; it is not a probability of approval.");
    }

    private Double findMinimumGpa(String text){
        List<Pattern> patterns=List.of(
                Pattern.compile("(?i)minimum\\s+(?:required\\s+)?(?:gpa|cgpa)(?:\\s+of)?\\s*(?:is|:)?\\s*(\\d+(?:\\.\\d+)?)"),
                Pattern.compile("(?i)(?:gpa|cgpa)\\s+(?:must be|should be|of at least|minimum of|>=|at least)\\s*(\\d+(?:\\.\\d+)?)"),
                Pattern.compile("(?i)at least\\s+(\\d+(?:\\.\\d+)?)\\s+(?:gpa|cgpa)"));
        for(Pattern p:patterns){Matcher m=p.matcher(text);if(m.find())try{return Double.parseDouble(m.group(1));}catch(Exception ignored){}}
        return null;
    }
    private Integer findMinimumYear(String text){
        Matcher m=Pattern.compile("(?i)(?:students? (?:must be|should be)(?:\\s+enrolled)?(?:\\s+in)?|students? in|minimum (?:academic )?year(?: is)?|from)\\s*(first|second|third|fourth|1st|2nd|3rd|4th|[1-4])\\s*(?:year)?").matcher(text);
        if(!m.find())return null;return switch(m.group(1).toLowerCase(Locale.ROOT)){case "first","1st","1"->1;case "second","2nd","2"->2;case "third","3rd","3"->3;case "fourth","4th","4"->4;default->null;};
    }
}
