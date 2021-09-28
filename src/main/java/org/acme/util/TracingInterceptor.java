package org.acme.util;

import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TracingInterceptor {

    // TODO add x-client-trace-id
    // TODO add https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_conn_man/headers#b3



    private static final List<String> FORWARDED_HEADER_NAMES = Arrays.asList
            ("x-request-id", "x-b3-traceid", "x-b3-spanid", "x-b3-parentspanid", "x-b3-sampled", "x-b3-flags", "x-ot-span-context", "x-cloud-trace-context", "traceparent", "tracestate", "b3");

    private static final String X_TRACING_HEADERS = "X-Tracing-Headers";

    private TracingInterceptor() {
        // Avoid direct instantiation.
    }

    public static Consumer<RoutingContext> create() {
        return new Consumer<RoutingContext>() {
            @Override
            public void accept(RoutingContext rc) {
                Set<String> names = rc.request().headers().names();
                Map<String, List<String>> headers = names.stream().map(String::toLowerCase)
                        .filter(FORWARDED_HEADER_NAMES::contains).collect(Collectors.toMap(Function.identity(),
                                h -> Collections.singletonList(rc.request().getHeader(h))));
                rc.put(X_TRACING_HEADERS, headers);
                rc.next();
            }
        };
    }

    public static WebClient propagate(WebClient client, RoutingContext rc) {
        WebClientInternal delegate = (WebClientInternal) client.getDelegate();
        delegate.addInterceptor(ctx -> {
            Map<String, List<String>> headers = rc.get(X_TRACING_HEADERS);
            if (headers != null) {
                System.out.println("Propagating... " + headers);
                headers.forEach((s, l) -> l.forEach(v -> ctx.request().putHeader(s, v)));
            }
            ctx.next();
        });
        return client;
    }
}