package org.acme.config;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JsonCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        // Support java.util.Optional fields, and hide those fields in the JSON response when empty
        mapper.registerModule(new Jdk8Module().configureAbsentsAsNulls(true));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }
}
