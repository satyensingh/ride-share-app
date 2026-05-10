package com.example.rideshare.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final Converter<Jwt, AbstractAuthenticationToken> converter =
            new SecurityConfig().jwtAuthenticationConverter();

    @Test
    void jwtAuthenticationConverterShouldMapKeycloakRealmRolesToSpringAuthorities() {
        Jwt jwt = jwtWithRealmRoles("car_owner", "admin");

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CAR_OWNER", "ROLE_ADMIN");
    }

    @Test
    void jwtAuthenticationConverterShouldReturnNoAuthoritiesWhenRealmAccessIsMissing() {
        Jwt jwt = baseJwtBuilder()
                .claim("preferred_username", "owner1")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void jwtAuthenticationConverterShouldIgnoreNonStringRoles() {
        Jwt jwt = baseJwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("passenger", 10, true)))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PASSENGER");
    }

    private Jwt jwtWithRealmRoles(String... roles) {
        return baseJwtBuilder()
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
    }

    private Jwt.Builder baseJwtBuilder() {
        Instant issuedAt = Instant.parse("2026-05-10T10:00:00Z");
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("http://localhost:8081/realms/ride-share")
                .subject("owner1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600));
    }
}
