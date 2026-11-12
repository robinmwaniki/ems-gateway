package com.robinmwaniki.emsgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Writes one log line per request: correlation ID, method, path, status and
 * duration.
 *
 * <p>Query strings and headers are deliberately not logged, because they can
 * contain tokens and other secrets.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /**
     * Times the request and logs it once the response has finished.
     *
     * @param exchange the current request and response
     * @param chain    the rest of the filter chain
     * @return completes when the rest of the chain has completed
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();

        // doFinally runs for success, errors and cancelled requests alike.
        return chain.filter(exchange).doFinally(signal -> {
            String requestId = exchange.getAttributeOrDefault(CorrelationIdFilter.ATTRIBUTE, "-");
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            long millis = (System.nanoTime() - start) / 1_000_000;

            log.info("requestId={} {} {} -> {} in {} ms",
                    requestId,
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getRawPath(),
                    status == null ? "n/a" : status.value(),
                    millis);
        });
    }

    /**
     * Runs right after the correlation ID has been assigned.
     *
     * @return the filter order
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}