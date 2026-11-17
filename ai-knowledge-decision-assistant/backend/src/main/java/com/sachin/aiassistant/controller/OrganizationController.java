package com.sachin.aiassistant.controller;
import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.security.AppUserDetails;
import com.sachin.aiassistant.service.OrganizationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/organization")
public class OrganizationController {
    private final OrganizationService service; public OrganizationController(OrganizationService service){this.service=service;}
    private AppUserDetails user(Authentication a){return (AppUserDetails)a.getPrincipal();}
    @GetMapping("/assistant") public AssistantResponse assistant(Authentication a){return service.assistant(user(a).getOrganizationId());}
    @PostMapping("/assistant/regenerate") public AssistantResponse regenerate(Authentication a){return service.regenerate(user(a).getOrganizationId());}
    @GetMapping("/usage") public UsageResponse usage(Authentication a){return service.usage(user(a).getOrganizationId());}
}
