package com.sachin.aiassistant.security;
import com.sachin.aiassistant.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
public class AppUserDetails implements UserDetails {
    private final UUID userId, organizationId;
    private final String organizationName, fullName, email, password, role;
    public AppUserDetails(AppUser user) {
        userId=user.getId(); organizationId=user.getOrganization().getId(); organizationName=user.getOrganization().getName();
        fullName=user.getFullName(); email=user.getEmail(); password=user.getPassword(); role=user.getRole();
    }
    public UUID getUserId(){return userId;} public UUID getOrganizationId(){return organizationId;}
    public String getOrganizationName(){return organizationName;} public String getFullName(){return fullName;} public String getRole(){return role;}
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+role));}
    @Override public String getPassword(){return password;} @Override public String getUsername(){return email;}
    @Override public boolean isAccountNonExpired(){return true;} @Override public boolean isAccountNonLocked(){return true;}
    @Override public boolean isCredentialsNonExpired(){return true;} @Override public boolean isEnabled(){return true;}
}
