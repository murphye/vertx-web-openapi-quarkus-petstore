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

@ApplicationScoped
public class PetStoreRouter {

    @Inject
    Vertx vertx;

    @Inject
    PetStoreService petStoreService;

    void init(@Observes Router router) {
        router.route().handler(BodyHandler.create());
        final var routerBuilder = RouterBuilder.createAndAwait(vertx, "https://app.swaggerhub.com/apis/murphye/petstore/1.0.0-oas3");

        routerBuilder.operation("listPets").handler(this::listPets);
        routerBuilder.operation("createPets").handler(this::createPets);
        routerBuilder.operation("showPetById").handler(this::showPetById);

        final var apiRouter = routerBuilder.createRouter();

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