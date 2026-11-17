package com.sachin.aiassistant.config;
import com.sachin.aiassistant.security.CustomUserDetailsService;
import com.sachin.aiassistant.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter; private final CustomUserDetailsService userDetails;
    public SecurityConfig(JwtAuthenticationFilter jwtFilter,CustomUserDetailsService userDetails){this.jwtFilter=jwtFilter;this.userDetails=userDetails;}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean DaoAuthenticationProvider authenticationProvider(PasswordEncoder encoder){
        DaoAuthenticationProvider p=new DaoAuthenticationProvider(userDetails); p.setPasswordEncoder(encoder); return p;
    }
    @Bean AuthenticationManager authenticationManager(DaoAuthenticationProvider provider){return new ProviderManager(provider);}
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,DaoAuthenticationProvider provider)throws Exception{
        http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults())
          .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .exceptionHandling(e->e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
          .authenticationProvider(provider)
          .authorizeHttpRequests(a->a.requestMatchers("/api/health","/api/auth/register","/api/auth/login","/api/public/**","/widget","/widget/**","/","/assets/**","/favicon.ico").permitAll().anyRequest().authenticated())
          .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOriginPatterns(List.of("http://localhost:5173","http://localhost:3000","https://*.vercel.app"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
    }
}
