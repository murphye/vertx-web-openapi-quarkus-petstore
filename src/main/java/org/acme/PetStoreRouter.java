package org.acme;

import io.smallrye.mutiny.TimeoutException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.openapi.RouterBuilder;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class PetStoreRouter {

    private static final String SPEC_URL = "http://raw.githubusercontent.com/murphye/vertx-web-openapi-quarkus-petstore/main/src/main/resources/META-INF/openapi.yaml";

    @Inject
    Vertx vertx;

    @Inject
    PetStoreService petStoreService;

    void init(@Observes Router router) {
        // TODO: BodyHandler needed? 
        router.route().handler(BodyHandler.create());
        var routerBuilder = RouterBuilder.createAndAwait(vertx, SPEC_URL);

        routerBuilder.operation("listPets").handler(this::listPets);
        routerBuilder.operation("createPets").handler(this::createPets);
        routerBuilder.operation("showPetById").handler(this::showPetById);

        var apiRouter = routerBuilder.createRouter();

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

        proxyOpenApiSpec(apiRouter);

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

    /**
     * While the OpenAPI spec is available through the SPEC_URL, the spec URL may change when new versions of the application are deployed.
     * By proxying the OpenAPI spec here, it provides a URL to verify what OpenAPI spec is actually being served by the application.
     */
    private void proxyOpenApiSpec(Router router) {
        router.route(HttpMethod.GET, "/q/openapi").handler(rc -> {
            WebClient.create(vertx).getAbs(SPEC_URL).send() // Fetch the OpenAPI spec
                .ifNoItem().after(Duration.ofMillis(1000)).fail() // Fail after 1 second of waiting
                .onItem().invoke(response -> rc.response().setStatusCode(200).putHeader("Content-Type","text/yaml").endAndForget(response.bodyAsString()))
                .onFailure(TimeoutException.class).invoke(err -> rc.response().setStatusCode(500).endAndForget(err.getMessage()))
                .subscribe().with(System.out::println);
        });
    }
}