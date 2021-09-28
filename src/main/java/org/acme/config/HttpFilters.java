package org.acme.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

@ApplicationScoped
public class HttpFilters {

    public void registerFilters(@Observes Filters filters) {
        filters.register(rc -> {
            rc.response().putHeader("Cache-Control", "no-store, max-age=0");
            rc.next();
        }, 100);
    }
}
