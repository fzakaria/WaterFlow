package com.github.fzakaria.waterflow.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.immutables.value.Value;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * A wrapper for Jackson {@link ObjectMapper}.
 * It's highly recommended that the {@link ObjectMapper}
 * Default {@link ObjectMapper} is set with {@link com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping#OBJECT_AND_NON_CONCRETE}
 * which is absolutely necessary to serialize/deserialize complex POJOs
 */
@Value.Immutable
public abstract class JacksonDataConverter implements DataConverter {


    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    static {
        DEFAULT_MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        DEFAULT_MAPPER.registerModules(new Jdk8Module(), new GuavaModule(), new JavaTimeModule());
        DEFAULT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        DEFAULT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Value.Default
    public ObjectMapper objectMapper() {
        return DEFAULT_MAPPER;
    }

    @Override
    public String toData(Object input) throws DataConverterException {
        try {
            return objectMapper().writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new DataConverterException(e);
        }
    }

    @Override
    public <T> T fromData(String input, Type type) throws DataConverterException {
        try {
            return objectMapper().readValue(input, objectMapper().constructType(type));
        } catch (IOException e) {
            throw new DataConverterException(e);
        }
    }
}
