package com.robinmwaniki.emsgateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Unit tests for {@link KeycloakRoleConverter}. */
class KeycloakRoleConverterTest {

    /** Builds a token with the given extra claims. */
    private Jwt jwtWith(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void mapsRealmRolesToAuthoritiesWithTheRolePrefix() {
        Jwt jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", List.of("ADMIN", "HR_MANAGER"))));

        assertThat(KeycloakRoleConverter.extractRoles(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_HR_MANAGER");
    }

    @Test
    void returnsNoRolesWhenTheTokenHasNoRealmAccessClaim() {
        assertThat(KeycloakRoleConverter.extractRoles(jwtWith(Map.of()))).isEmpty();
    }

    @Test
    void returnsNoRolesWhenTheRolesEntryIsMissing() {
        Jwt jwt = jwtWith(Map.of("realm_access", Map.of("other", "value")));

        assertThat(KeycloakRoleConverter.extractRoles(jwt)).isEmpty();
    }

    @Test
    void returnsNoRolesWhenRolesIsNotAList() {
        Jwt jwt = jwtWith(Map.of("realm_access", Map.of("roles", "ADMIN")));

        assertThat(KeycloakRoleConverter.extractRoles(jwt)).isEmpty();
    }

    @Test
    void ignoresRoleValuesThatAreNotStrings() {
        Jwt jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", Arrays.asList("EMPLOYEE", 42, null))));

        assertThat(KeycloakRoleConverter.extractRoles(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EMPLOYEE");
    }
}