package com.sachin.aiassistant.security;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final CustomUserDetailsService users;
    public JwtAuthenticationFilter(JwtService jwt, CustomUserDetailsService users){this.jwt=jwt;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header==null || !header.startsWith("Bearer ")){chain.doFilter(request,response);return;}
        try {
            String token=header.substring(7); String email=jwt.extractUsername(token);
            if(email!=null && SecurityContextHolder.getContext().getAuthentication()==null){
                UserDetails user=users.loadUserByUsername(email);
                if(jwt.isTokenValid(token,user)){
                    var auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch(JwtException | IllegalArgumentException | AuthenticationException ignored) { /* protected routes reject unauthenticated requests */ }
        chain.doFilter(request,response);
    }
}
