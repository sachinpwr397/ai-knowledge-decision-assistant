package com.sachin.aiassistant.controller;
import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.repository.OrganizationRepository;
import com.sachin.aiassistant.service.QuestionProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/public")
public class PublicAssistantController {
    private final OrganizationRepository organizations; private final QuestionProcessingService questions;
    public PublicAssistantController(OrganizationRepository organizations,QuestionProcessingService questions){this.organizations=organizations;this.questions=questions;}
    @GetMapping("/assistant/{token}") public PublicAssistantResponse assistant(@PathVariable String token){
        var org=organizations.findByAssistantToken(token).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Assistant not found."));
        return new PublicAssistantResponse(org.getName(),"Ask a question about the documents provided by "+org.getName()+".");
    }
    @PostMapping("/ask") public AskResponse ask(@Valid @RequestBody AskRequest request){return questions.ask(request);}
}
