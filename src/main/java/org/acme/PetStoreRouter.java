package org.acme;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.openapi.RouterBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

//TODO: use x-request-id as Idempotency key https://medium.com/@saurav200892/how-to-achieve-idempotency-in-post-method-d88d7b08fcdd ? 
// https://datatracker.ietf.org/doc/html/draft-ietf-httpapi-idempotency-key-header-00
// https://docs.webengage.com/docs/webhooks (Uses x-request-id)
// https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/observability/tracing


@ApplicationScoped
public class PetStoreRouter {

    static final String SPEC_URL = "https://app.swaggerhub.com/apis/murphye/petstore/1.0.0-oas3";

    @Inject
    Vertx vertx;

    @Inject
    PetStoreService petStoreService;

    void init(@Observes Router router) {
        //router.route().handler(TracingInterceptor.create());
        router.route().handler(BodyHandler.create());

        // Problem with Native https://github.com/vert-x3/vertx-web/issues/2045
        var routerBuilder = RouterBuilder.createAndAwait(vertx, SPEC_URL);

        routerBuilder.operation("listPets").handler(this::listPets);
        routerBuilder.operation("createPets").handler(this::createPets);
        routerBuilder.operation("showPetById").handler(this::showPetById);

        Router apiRouter = routerBuilder.createRouter();

        apiRouter.errorHandler(404, rc -> {
            JsonObject errorJson = new JsonObject().put("code", 404).put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Not Found");
            rc.response().setStatusCode(404).endAndForget(errorJson.encode());
        });

        apiRouter.errorHandler(400, rc -> {
            JsonObject errorJson = new JsonObject().put("code", 400).put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Validation Exception");
            rc.response().setStatusCode(400).endAndForget(errorJson.encode());
        });

        router.mountSubRouter("/", apiRouter);
    }

    void listPets(RoutingContext rc) {
        var petJson = new JsonArray(this.petStoreService.listPets());
        rc.response().setStatusCode(200).endAndForget(petJson.encode());
    }

    void createPets(RoutingContext rc) {
        Pet pet = rc.getBodyAsJson().mapTo(Pet.class);
        this.petStoreService.createPets(pet);
        rc.response().setStatusCode(200).endAndForget();
    }

    void showPetById(RoutingContext rc) {
        var id = Integer.parseInt(rc.pathParam("petId"));
        var pet = this.petStoreService.showPetById(id);

        if (pet.isPresent()) {
            var petJson = JsonObject.mapFrom(pet.get());
            rc.response().setStatusCode(200).endAndForget(petJson.encode());
        } else
            rc.fail(404, new Exception("Pet not found"));
    }
}