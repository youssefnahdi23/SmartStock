package com.smartstock.product.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class SecurityUserDetails implements UserDetails {

    private final String userId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserDetails(String userId, String username,
                                List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.authorities = Stream.concat(
                        roles != null ? roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)) : Stream.empty(),
                        permissions != null ? permissions.stream().map(SimpleGrantedAuthority::new) : Stream.empty())
                .collect(Collectors.toList());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
