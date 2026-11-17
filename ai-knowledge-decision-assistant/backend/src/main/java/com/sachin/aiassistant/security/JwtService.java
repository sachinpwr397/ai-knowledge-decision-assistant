package com.sachin.aiassistant.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
@Service
public class JwtService {
    private final SecretKey key; private final long expirationSeconds;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        if(secret==null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured with a Base64-encoded key of at least 32 bytes.");
        this.key=Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); this.expirationSeconds=expirationSeconds;
    }
    public String generateToken(AppUserDetails user) {
        Instant now=Instant.now();
        return Jwts.builder().claims(Map.of("userId",user.getUserId().toString(),"organizationId",user.getOrganizationId().toString(),"role",user.getRole()))
                .subject(user.getUsername()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }
    public String extractUsername(String token){return claims(token).getSubject();}
    public boolean isTokenValid(String token, UserDetails user){Claims c=claims(token); return c.getSubject().equalsIgnoreCase(user.getUsername()) && c.getExpiration().after(new Date());}
    public long getExpirationSeconds(){return expirationSeconds;}
    private Claims claims(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
}
