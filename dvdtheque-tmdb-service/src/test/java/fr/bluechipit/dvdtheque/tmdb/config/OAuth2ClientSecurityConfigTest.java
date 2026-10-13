package fr.bluechipit.dvdtheque.tmdb.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2ClientSecurityConfigTest {

    private final OAuth2ClientSecurityConfig config = new OAuth2ClientSecurityConfig();

    @Test
    @DisplayName("realm_access roles are mapped to ROLE_ authorities")
    void shouldMapRealmAccessRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("user", "batch")))
                .build();

        Collection<GrantedAuthority> authorities = config.jwtGrantedAuthoritiesConverter().convert(jwt);

        assertTrue(authorities.stream().anyMatch(a -> "ROLE_user".equals(a.getAuthority())));
        assertTrue(authorities.stream().anyMatch(a -> "ROLE_batch".equals(a.getAuthority())));
    }

    @Test
    @DisplayName("no realm_access claim keeps the default scope authorities")
    void shouldKeepDelegateAuthoritiesWithoutRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "read")
                .build();

        Collection<GrantedAuthority> authorities = config.jwtGrantedAuthoritiesConverter().convert(jwt);

        assertTrue(authorities.stream().anyMatch(a -> "SCOPE_read".equals(a.getAuthority())));
    }

    @Test
    @DisplayName("jwtAuthenticationConverter builds a token exposing the mapped roles")
    void jwtAuthenticationConverterShouldExposeAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .build();

        AbstractAuthenticationToken token = config.jwtAuthenticationConverter().convert(jwt);

        assertNotNull(token);
        assertTrue(token.getAuthorities().stream().anyMatch(a -> "ROLE_user".equals(a.getAuthority())));
    }
}
