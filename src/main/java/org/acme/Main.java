package org.acme;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.openapi.RouterBuilder;
import io.vertx.mutiny.ext.web.validation.RequestParameters;
import io.vertx.mutiny.ext.web.validation.ValidationHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@ApplicationScoped
public class Main {

    @Inject Vertx vertx;

    final List<Pet> pets = new ArrayList<>(Arrays.asList(
        new Pet(1, "Fufi", Optional.of("ABC")),
        new Pet(2, "Garfield", Optional.of("XYZ")),
        new Pet(3, "Puffa", Optional.empty())
    ));

    void init(@Observes io.vertx.ext.web.Router router) {

        RouterBuilder routerBuilder = RouterBuilder.createAndAwait(vertx, "META-INF/openapi.yaml");
        routerBuilder.operation("listPets").handler(this::listPets);
        routerBuilder.operation("createPets").handler(this::createPets);
        routerBuilder.operation("showPetById").handler(this::showPetById);
   
        Router apiRouter = routerBuilder.createRouter();

        apiRouter.errorHandler(404, rc -> {
            JsonObject errorObject = new JsonObject()
                    .put("code", 404)
                    .put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Not Found");
                    rc.response().setStatusCode(404).putHeader(CONTENT_TYPE, APPLICATION_JSON).endAndForget(errorObject.encode());
        });

        apiRouter.errorHandler(400, rc -> {
            JsonObject errorObject = new JsonObject()
                    .put("code", 400)
                    .put("message",
                    (rc.failure() != null) ? rc.failure().getMessage() : "Validation Exception");
                    rc.response().setStatusCode(400).putHeader(CONTENT_TYPE, APPLICATION_JSON).endAndForget(errorObject.encode());
        });

        Router.newInstance(router).mountSubRouter("/", apiRouter); // Convert to Mutiny Router and mount apiRouter at '/'
    }

    void listPets(RoutingContext rc) {
        rc.response().setStatusCode(200).putHeader(CONTENT_TYPE, APPLICATION_JSON).endAndForget(new JsonArray(this.pets).encode());
    }

    void createPets(RoutingContext rc) {
        RequestParameters params = rc.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        Pet pet = params.body().getJsonObject().mapTo(Pet.class);
        this.pets.add(pet);
        rc.response().setStatusCode(200).endAndForget();
    }

    void showPetById(RoutingContext rc) {
        Integer id = Integer.parseInt(rc.pathParams().get("petId"));

        Optional<Pet> pet = this.pets.stream().filter(p -> p.id().equals(id)).findFirst();

        if (pet.isPresent()) {
          var jsonObject = JsonObject.mapFrom(pet.get());
          rc.response().setStatusCode(200).putHeader(CONTENT_TYPE, APPLICATION_JSON).endAndForget(jsonObject.encode());
        }
        else
          rc.fail(404, new Exception("Pet not found"));
    }
}