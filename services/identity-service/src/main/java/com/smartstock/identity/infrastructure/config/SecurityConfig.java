package com.smartstock.identity.infrastructure.config;

import com.smartstock.identity.infrastructure.security.JwtAuthenticationFilter;
import com.smartstock.identity.infrastructure.security.LoginRateLimitFilter;
import com.smartstock.identity.infrastructure.security.RestAccessDeniedHandler;
import com.smartstock.identity.infrastructure.security.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final SecurityProperties securityProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          LoginRateLimitFilter loginRateLimitFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler,
                          SecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, LoginRateLimitFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(securityProperties.getCors().getAllowedMethods());
        configuration.setAllowedHeaders(securityProperties.getCors().getAllowedHeaders());
        configuration.setExposedHeaders(securityProperties.getCors().getExposedHeaders());
        configuration.setAllowCredentials(securityProperties.getCors().isAllowCredentials());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
