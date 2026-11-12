package com.robinmwaniki.emsgateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Makes sure every request passing through the gateway carries a correlation ID.
 *
 * <p>The ID is forwarded to the downstream service and returned to the caller in
 * the {@value #HEADER} header, so one request can be followed through the
 * gateway, the backend and the logs.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    /** Name of the header that carries the correlation ID. */
    public static final String HEADER = "X-Request-Id";

    /** Exchange attribute under which the ID is stored for other filters. */
    public static final String ATTRIBUTE = "ems.requestId";

    // Only short, plain IDs are accepted from clients, so nobody can inject
    // unusual characters into the logs.
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    /**
     * Keeps a valid incoming ID or generates a new one, then forwards it.
     *
     * @param exchange the current request and response
     * @param chain    the rest of the filter chain
     * @return completes when the rest of the chain has completed
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = isValid(incoming) ? incoming : UUID.randomUUID().toString();

        // Request headers are read-only, so build a copy that carries the ID downstream.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, requestId))
                .build();

        // Return the same ID to the caller so it can be quoted in bug reports.
        exchange.getResponse().getHeaders().set(HEADER, requestId);
        exchange.getAttributes().put(ATTRIBUTE, requestId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * Runs before every other gateway filter.
     *
     * @return the filter order
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Checks whether a client-supplied ID is safe to reuse.
     *
     * @param value the incoming header value, may be null
     * @return true if the value is present and matches the allowed format
     */
    static boolean isValid(String value) {
        return value != null && VALID_ID.matcher(value).matches();
    }
}