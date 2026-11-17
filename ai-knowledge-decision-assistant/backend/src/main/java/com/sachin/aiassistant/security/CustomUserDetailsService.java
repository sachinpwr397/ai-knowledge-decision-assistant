package com.sachin.aiassistant.security;
import com.sachin.aiassistant.repository.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;
    public CustomUserDetailsService(AppUserRepository users){this.users=users;}
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(email).map(AppUserDetails::new).orElseThrow(()->new UsernameNotFoundException("User not found"));
    }
}
