package org.acme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PetStoreService {

    final List<Pet> pets = new ArrayList<>(Arrays.asList(
        new Pet(1, "Fufi", Optional.of("ABC")),
        new Pet(2, "Garfield", Optional.of("XYZ")),
        new Pet(3, "Puffa", Optional.empty())
    ));

    List<Pet> listPets() {
        return this.pets;
    }

    void createPets(Pet pet) {
        this.pets.add(pet);
    }

    Optional<Pet> showPetById(int id) {
        return this.pets.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
