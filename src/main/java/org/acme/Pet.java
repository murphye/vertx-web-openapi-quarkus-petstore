package org.acme;

import java.util.Optional;

public record Pet(Integer id, String name, Optional<String> tag) {}
