package com.smartstock.identity.infrastructure.security;

import com.smartstock.identity.presentation.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                AuthenticatedUser authenticatedUser = jwtTokenProvider.toAuthenticatedUser(token);
                List<SimpleGrantedAuthority> authorities = buildAuthorities(authenticatedUser);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                RequestContextHolder.get().ifPresent(context -> {
                    RequestContextHolder.set(context.withAuthenticatedUser(authenticatedUser.userId().toString(), authenticatedUser.username()));
                    MDC.put("userId", authenticatedUser.userId().toString());
                });
            } catch (InvalidTokenException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> buildAuthorities(AuthenticatedUser authenticatedUser) {
        List<SimpleGrantedAuthority> roleAuthorities = authenticatedUser.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        List<SimpleGrantedAuthority> permissionAuthorities = authenticatedUser.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return java.util.stream.Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream()).toList();
    }
}
