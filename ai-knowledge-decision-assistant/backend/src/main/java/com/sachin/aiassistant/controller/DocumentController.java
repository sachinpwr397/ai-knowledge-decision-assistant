package com.sachin.aiassistant.controller;
import com.sachin.aiassistant.dto.DocumentResponse;
import com.sachin.aiassistant.security.AppUserDetails;
import com.sachin.aiassistant.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service; public DocumentController(DocumentService service){this.service=service;}
    private UUID organizationId(Authentication a){return ((AppUserDetails)a.getPrincipal()).getOrganizationId();}
    @GetMapping public List<DocumentResponse> list(Authentication a){return service.list(organizationId(a));}
    @PostMapping(consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestPart("file") MultipartFile file,Authentication a){return service.upload(organizationId(a),file);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,Authentication a){service.delete(organizationId(a),id);}
}
