package org.example.apisecurise.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // active @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ← OBLIGATOIRE


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

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173")); // ton frontend
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

}