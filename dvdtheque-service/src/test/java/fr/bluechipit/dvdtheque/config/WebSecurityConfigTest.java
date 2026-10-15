package fr.bluechipit.dvdtheque.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSecurityConfig - Unit Tests")
class WebSecurityConfigTest {

    private final WebSecurityConfig config = new WebSecurityConfig();

    private static Jwt jwtWithRealmRoles() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "read")
                .claim("realm_access", Map.of("roles", List.of("user", "dvdtheque")))
                .build();
    }

    @Test
    @DisplayName("jwtGrantedAuthoritiesConverter doit ajouter les rôles realm_access en ROLE_")
    void grantedAuthorities_ShouldMapRealmAccessRoles() {
        Converter<Jwt, Collection<GrantedAuthority>> converter = config.jwtGrantedAuthoritiesConverter();

        Collection<GrantedAuthority> authorities = converter.convert(jwtWithRealmRoles());

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_user", "ROLE_dvdtheque", "SCOPE_read");
    }

    @Test
    @DisplayName("jwtGrantedAuthoritiesConverter doit se contenter du delegate sans realm_access")
    void grantedAuthorities_ShouldFallbackToDelegateWhenNoRealmAccess() {
        Converter<Jwt, Collection<GrantedAuthority>> converter = config.jwtGrantedAuthoritiesConverter();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("scope", "read").build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_read");
    }

    @Test
    @DisplayName("jwtAuthenticationConverter doit produire un JwtAuthenticationToken avec les rôles")
    void jwtAuthenticationConverter_ShouldBuildToken() {
        Converter<Jwt, AbstractAuthenticationToken> converter = config.jwtAuthenticationConverter();

        AbstractAuthenticationToken token = converter.convert(jwtWithRealmRoles());

        assertThat(token).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_user", "ROLE_dvdtheque");
    }
}
