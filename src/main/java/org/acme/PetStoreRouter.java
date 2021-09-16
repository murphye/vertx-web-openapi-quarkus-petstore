package org.acme;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
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

    void init(@Observes io.vertx.ext.web.Router router) {
        RouterBuilder routerBuilder = RouterBuilder.createAndAwait(vertx, "META-INF/openapi.yaml");
        
        routerBuilder.operation("listPets").handler(this::listPets);
        routerBuilder.operation("createPets").handler(this::createPets);
        routerBuilder.operation("showPetById").handler(this::showPetById);

        Router apiRouter = routerBuilder.createRouter();

        apiRouter.errorHandler(404, rc -> {
            JsonObject errorObject = new JsonObject().put("code", 404).put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Not Found");
            rc.response().setStatusCode(404).endAndForget(errorObject.encode());
        });

        apiRouter.errorHandler(400, rc -> {
            JsonObject errorObject = new JsonObject().put("code", 400).put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Validation Exception");
            rc.response().setStatusCode(400).endAndForget(errorObject.encode());
        });

        Router.newInstance(router).mountSubRouter("/", apiRouter);
    }

    void listPets(RoutingContext rc) {
        rc.response().setStatusCode(200).endAndForget(new JsonArray(this.petStoreService.listPets()).encode());
    }

    void createPets(RoutingContext rc) {
        Pet pet = rc.getBodyAsJson().mapTo(Pet.class);
        this.petStoreService.createPets(pet);
        rc.response().setStatusCode(200).endAndForget();
    }

    void showPetById(RoutingContext rc) {
        var id = Integer.parseInt(rc.pathParams().get("petId"));
        var pet = this.petStoreService.showPetById(id);

        if (pet.isPresent()) {
            rc.response().setStatusCode(200).endAndForget(JsonObject.mapFrom(pet.get()).encode());
        } else
            rc.fail(404, new Exception("Pet not found"));
    }
}