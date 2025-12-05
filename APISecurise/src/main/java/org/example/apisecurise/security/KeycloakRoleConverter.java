package org.example.apisecurise.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<SimpleGrantedAuthority>> {
    @Override
    public Collection<SimpleGrantedAuthority> convert(Jwt jwt) {
        var realmAccess = (java.util.Map<String,Object>) jwt.getClaim("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) return List.of();
        var roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }
}