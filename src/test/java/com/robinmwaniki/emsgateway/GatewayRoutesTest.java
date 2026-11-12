package com.robinmwaniki.emsgateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * Loads the full application and checks that the routes in application.yml
 * were picked up. This also catches a wrong property name after a Spring
 * Cloud upgrade, because the routes would silently be missing.
 */
@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    private List<Route> routes() {
        return routeLocator.getRoutes().collectList().block();
    }

    @Test
    void loadsAllConfiguredRoutes() {
        List<String> ids = routes().stream().map(Route::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder("ems-api", "finance", "frontend");
    }

    @Test
    void checksTheCatchAllFrontendRouteLast() {
        List<Route> routes = routes();
        int frontendOrder = routes.stream()
                .filter(route -> route.getId().equals("frontend"))
                .findFirst()
                .orElseThrow()
                .getOrder();

        assertThat(routes.stream()
                .filter(route -> !route.getId().equals("frontend"))
                .map(Route::getOrder))
                .allMatch(order -> order < frontendOrder);
    }
}