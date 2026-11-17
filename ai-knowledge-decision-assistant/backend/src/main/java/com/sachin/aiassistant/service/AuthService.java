package com.sachin.aiassistant.service;
import com.sachin.aiassistant.dto.*;
import com.sachin.aiassistant.entity.*;
import com.sachin.aiassistant.repository.*;
import com.sachin.aiassistant.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AuthService {
    private final AppUserRepository users; private final OrganizationRepository organizations; private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager; private final JwtService jwt;
    @Value("${app.public-base-url}") private String publicBaseUrl;
    public AuthService(AppUserRepository users,OrganizationRepository organizations,PasswordEncoder encoder,AuthenticationManager authenticationManager,JwtService jwt){
        this.users=users;this.organizations=organizations;this.encoder=encoder;this.authenticationManager=authenticationManager;this.jwt=jwt;
    }
    @Transactional public AuthResponse register(RegisterRequest r){
        String email=r.email().trim().toLowerCase();
        if(users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
        Organization org=organizations.save(new Organization(r.organizationName().trim()));
        AppUser saved=users.save(new AppUser(r.fullName().trim(),email,encoder.encode(r.password()),"ORG_ADMIN",org));
        return response(new AppUserDetails(saved));
    }
    @Transactional(readOnly=true) public AuthResponse login(LoginRequest r){
        String email=r.email().trim().toLowerCase();
        try{authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,r.password()));}
        catch(AuthenticationException ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password.");}
        AppUser user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password."));
        return response(new AppUserDetails(user));
    }
    private AuthResponse response(AppUserDetails u){
        Organization org=organizations.findById(u.getOrganizationId()).orElseThrow();
        return new AuthResponse(jwt.generateToken(u),"Bearer",jwt.getExpirationSeconds(),u.getUserId(),u.getFullName(),u.getUsername(),u.getRole(),u.getOrganizationId(),u.getOrganizationName(),org.getAssistantToken());
    }
}
