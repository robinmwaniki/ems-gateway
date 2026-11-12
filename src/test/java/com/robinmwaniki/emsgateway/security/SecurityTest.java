package com.robinmwaniki.emsgateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Checks the security rules of the gateway against the full application.
 *
 * <p>The backend address points at a port nobody listens on, so a request that
 * passes security fails at the proxy step with a gateway error instead of
 * depending on a real backend.
 */
@SpringBootTest(properties = "EMS_API_URL=http://localhost:1")
class SecurityTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void rejectsApiRequestsWithoutAToken() {
        client.get().uri("/api/employees")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void allowsTheHealthEndpointWithoutAToken() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void letsAuthenticatedApiRequestsPastSecurity() {
        client.mutateWith(mockJwt())
                .get().uri("/api/employees")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
    }
}