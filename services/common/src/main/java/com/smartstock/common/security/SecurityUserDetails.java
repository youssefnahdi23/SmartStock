package com.smartstock.common.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated principal derived from a validated JWT (debt H-1). Permissions are exposed as
 * {@code PERMISSION_*} authorities so {@code @PreAuthorize("hasAuthority('PERMISSION_...')")}
 * works uniformly across services.
 */
@Getter
@Builder
public class SecurityUserDetails implements UserDetails {

    private String userId;
    private String email;
    private List<String> permissions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(p -> new SimpleGrantedAuthority("PERMISSION_" + p))
                .toList();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
