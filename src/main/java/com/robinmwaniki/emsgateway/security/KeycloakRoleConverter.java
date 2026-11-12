package com.robinmwaniki.emsgateway.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns the realm roles in a Keycloak access token into Spring Security
 * authorities.
 *
 * <p>Keycloak puts realm roles in the {@code realm_access.roles} claim, for
 * example {@code ["ADMIN", "HR_MANAGER"]}. Each one becomes an authority with
 * the {@code ROLE_} prefix, such as {@code ROLE_ADMIN}, which matches the role
 * names used by the Employee Management System.
 */
public final class KeycloakRoleConverter {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private KeycloakRoleConverter() {
        // Utility class, not meant to be instantiated.
    }

    /**
     * Reads the realm roles from a token.
     *
     * @param jwt the validated access token
     * @return one authority per role, or an empty list if the token has no roles
     */
    public static List<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get(ROLES_KEY);
        if (!(roles instanceof Collection<?> values)) {
            return List.of();
        }

        // Anything that is not a string is ignored rather than trusted.
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }
}