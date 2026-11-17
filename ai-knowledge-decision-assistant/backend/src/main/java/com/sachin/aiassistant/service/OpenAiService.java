package com.sachin.aiassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sachin.aiassistant.dto.DecisionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiService {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final ObjectMapper mapper;

    public OpenAiService(@Value("${app.openai.api-key:}") String apiKey,
                         @Value("${app.openai.model:gpt-4o-mini}") String model,
                         ObjectMapper mapper) {
        this.apiKey=apiKey; this.model=model; this.mapper=mapper;
        this.client=RestClient.builder().baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + (apiKey == null ? "" : apiKey)).build();
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public QuestionAnalysis analyzeQuestion(String question, String evidence) {
        QuestionAnalysis fallback = fallbackAnalysis(question);
        if (!isConfigured()) return fallback;
        try {
            String system = "You are a question parser for a document-grounded assistant. Return only a JSON object with keys intent (INFORMATION or DECISION), policyCategory (MERIT, SPORTS, NEED_BASED, GENERAL), academicYear (integer or null), gpa (number or null), hasOtherScholarship (boolean or null), and decisionChecks (array of short independent checks such as GPA threshold, academic year, existing scholarship restriction, full-time enrollment). Extract only facts explicitly stated by the user. Do not decide eligibility. Treat the document context as policy context, not as user facts. No chain-of-thought.";
            String user = "Question: " + question + "\nPolicy evidence:\n" + evidence;
            String content = chat(system, user, true);
            Map<?,?> parsed = mapper.readValue(stripCodeFence(content), Map.class);
            return new QuestionAnalysis(string(parsed.get("intent"), fallback.intent()),
                    string(parsed.get("policyCategory"), fallback.policyCategory()),
                    integer(parsed.get("academicYear"), fallback.academicYear()),
                    decimal(parsed.get("gpa"), fallback.gpa()),
                    bool(parsed.get("hasOtherScholarship"), fallback.hasOtherScholarship()),
                    stringList(parsed.get("decisionChecks"), fallback.decisionChecks()));
        } catch (Exception ignored) { return fallback; }
    }

    public Optional<String> generateAnswer(String question, String evidence, List<DecisionResult> decisions) {
        if (!isConfigured()) return Optional.empty();
        try {
            String system = "You answer questions using only the supplied retrieved document excerpts and structured decision results. If the excerpts do not support an answer, say the knowledge base does not contain enough information. Never invent policy terms, dates, limits, or citations. Mention source filenames and page numbers when relevant. Summarize decision outcomes and their short rationales without revealing private chain-of-thought. Be concise and helpful.";
            String user = "Question: " + question + "\nRetrieved excerpts:\n" + evidence + "\nStructured decision results:\n" + mapper.writeValueAsString(decisions);
            return Optional.of(chat(system, user, false));
        } catch (Exception ignored) { return Optional.empty(); }
    }

    private String chat(String system, String user, boolean json) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("max_tokens", 650);
        body.put("messages", List.of(Map.of("role","system","content",system), Map.of("role","user","content",user)));
        if (json) body.put("response_format", Map.of("type","json_object"));
        Map<?,?> response = client.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Map.class);
        if (response == null) throw new IllegalStateException("Empty AI response");
        List<?> choices=(List<?>)response.get("choices");
        if (choices == null || choices.isEmpty()) throw new IllegalStateException("No AI choices returned");
        Map<?,?> choice=(Map<?,?>)choices.get(0); Map<?,?> message=(Map<?,?>)choice.get("message");
        Object content=message==null?null:message.get("content");
        if (!(content instanceof String text) || text.isBlank()) throw new IllegalStateException("Empty AI answer");
        return text.trim();
    }

    private QuestionAnalysis fallbackAnalysis(String question) {
        String q=question.toLowerCase(Locale.ROOT);
        boolean decision = q.matches("(?s).*(eligible|eligibility|qualify|qualification|should i apply|can i receive|can i get|compare|which scholarship).*" );
        String category = q.contains("merit") || q.contains("gpa") || q.contains("grade") ? "MERIT"
                : q.contains("sports") || q.contains("athlet") ? "SPORTS"
                : q.contains("need") || q.contains("income") || q.contains("financial") ? "NEED_BASED" : "GENERAL";
        Integer year = null;
        Matcher yearMatcher=Pattern.compile("(?i)(?:year\\s*)?(1st|first|2nd|second|3rd|third|4th|fourth|[1-4])\\s*(?:year)?").matcher(q);
        if(yearMatcher.find()){String v=yearMatcher.group(1).toLowerCase(Locale.ROOT);year=switch(v){case "1st","first"->1;case "2nd","second"->2;case "3rd","third"->3;case "4th","fourth"->4;default->Integer.parseInt(v);};}
        Double gpa=null; Matcher gm=Pattern.compile("(?i)(?:gpa|cgpa)\\s*(?:is|of|=|:)?\\s*(\\d+(?:\\.\\d+)?)").matcher(q);
        if(gm.find())try{gpa=Double.parseDouble(gm.group(1));}catch(NumberFormatException ignored){}
        Boolean other=q.matches("(?s).*(do not|don't|dont|no|without|not receiving|not receive).{0,45}(another|other) scholarship.*") ? Boolean.FALSE
                : q.contains("sports scholarship") || q.contains("another scholarship") || q.contains("other scholarship") ? Boolean.TRUE : null;
        List<String> checks = decision ? List.of("policy category", "GPA threshold", "academic year", "other scholarship restriction") : List.of();
        return new QuestionAnalysis(decision?"DECISION":"INFORMATION",category,year,gpa,other,checks);
    }
    private String stripCodeFence(String s){return s.replaceAll("(?s)^```(?:json)?\\s*","").replaceAll("\\s*```$","").trim();}
    private String string(Object v,String fallback){return v instanceof String s && !s.isBlank()?s:fallback;}
    private Integer integer(Object v,Integer fallback){if(v instanceof Number n)return n.intValue();try{return v==null?fallback:Integer.valueOf(v.toString());}catch(Exception e){return fallback;}}
    private Double decimal(Object v,Double fallback){if(v instanceof Number n)return n.doubleValue();try{return v==null?fallback:Double.valueOf(v.toString());}catch(Exception e){return fallback;}}
    private Boolean bool(Object v,Boolean fallback){return v instanceof Boolean b?b:fallback;}
    private List<String> stringList(Object v,List<String> fallback){
        if(v instanceof List<?> values){List<String> out=values.stream().filter(String.class::isInstance).map(String.class::cast).toList();return out.isEmpty()?fallback:out;}
        return fallback;
    }
}
