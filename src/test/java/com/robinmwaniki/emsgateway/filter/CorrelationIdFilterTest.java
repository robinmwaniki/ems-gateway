package com.robinmwaniki.emsgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import com.robinmwaniki.emsgateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/** Unit tests for {@link CorrelationIdFilter}. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    /** Runs the filter and returns the exchange that reached the end of the chain. */
    private ServerWebExchange run(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwarded.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        return forwarded.get();
    }

    private MockServerWebExchange exchangeWithId(String id) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/employees")
                        .header(CorrelationIdFilter.HEADER, id)
                        .build());
    }

    @Test
    void generatesAnIdWhenTheRequestHasNone() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/employees").build());

        ServerWebExchange forwarded = run(exchange);

        String sent = forwarded.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER);
        assertThat(sent).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo(sent);
    }

    @Test
    void keepsAValidIncomingId() {
        MockServerWebExchange exchange = exchangeWithId("abc12345-def6");

        ServerWebExchange forwarded = run(exchange);

        assertThat(forwarded.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo("abc12345-def6");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo("abc12345-def6");
    }

    @Test
    void replacesAnIdWithUnsafeCharacters() {
        ServerWebExchange forwarded = run(exchangeWithId("not valid!!"));

        assertThat(forwarded.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isNotEqualTo("not valid!!")
                .matches("^[A-Za-z0-9-]{8,64}$");
    }

    @Test
    void replacesAnIdThatIsTooShort() {
        ServerWebExchange forwarded = run(exchangeWithId("abc"));

        assertThat(forwarded.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isNotEqualTo("abc");
    }

    @Test
    void storesTheIdForOtherFilters() {
        ServerWebExchange forwarded = run(exchangeWithId("abc12345-def6"));

        assertThat(forwarded.<String>getAttribute(CorrelationIdFilter.ATTRIBUTE))
                .isEqualTo("abc12345-def6");
    }
}