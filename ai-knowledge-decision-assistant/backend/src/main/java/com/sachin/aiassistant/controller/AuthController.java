package com.sachin.aiassistant.controller;
import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.security.AppUserDetails;
import com.sachin.aiassistant.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthResponse register(@Valid @RequestBody RegisterRequest r){return auth.register(r);}
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r){return auth.login(r);}
    @GetMapping("/me") public UserProfileResponse me(Authentication a){
        AppUserDetails u=(AppUserDetails)a.getPrincipal(); return new UserProfileResponse(u.getUserId(),u.getFullName(),u.getUsername(),u.getRole(),u.getOrganizationId(),u.getOrganizationName());
    }
}
