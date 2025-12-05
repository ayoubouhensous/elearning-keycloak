package org.example.apisecurise.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // active @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var realmAccess = (java.util.Map<String, Object>) jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.get("roles") == null) {
                return java.util.List.of();
            }

            var roles = (java.util.List<String>) realmAccess.get("roles");

            return roles.stream()
                    .map(r -> {
                        String role = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                        return new org.springframework.security.core.authority.SimpleGrantedAuthority(role);
                    })
                    .collect(java.util.stream.Collectors.toList());
        });
        return converter;
    }

}