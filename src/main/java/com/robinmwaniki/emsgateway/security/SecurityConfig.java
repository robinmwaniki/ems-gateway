package com.robinmwaniki.emsgateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Flux;

/**
 * Security rules of the gateway.
 *
 * <p>Requests to {@code /api/**} must carry a valid Keycloak access token. The
 * token is checked here, at the edge, and is also forwarded to the backend,
 * which checks it again and decides what each role may do. The frontend files,
 * the health endpoint and CORS preflight requests are public.
 *
 * <p>The {@code /finance/**} route is not protected yet. It is covered when the
 * Student Finance app is connected to Keycloak.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Defines which requests are public and which need a token.
     *
     * @param http the security builder provided by Spring
     * @return the security filter chain
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // The API is stateless and uses bearer tokens, not cookies, so
                // there is no session for a CSRF attack to ride on.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Browsers send this without a token before cross-origin calls.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Maps Keycloak realm roles to Spring Security authorities.
     *
     * @return the converter used for every validated token
     */
    private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> Flux.fromIterable(KeycloakRoleConverter.extractRoles(jwt)));
        return converter;
    }
}