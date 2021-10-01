package org.acme;

import java.util.Optional;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Pet(Integer id, String name, Optional<String> tag) {}
