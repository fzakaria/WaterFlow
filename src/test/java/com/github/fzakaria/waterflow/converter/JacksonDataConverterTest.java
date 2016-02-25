package com.github.fzakaria.waterflow.converter;

import com.google.common.reflect.TypeToken;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class JacksonDataConverterTest {

    private final JacksonDataConverter dataConverter = ImmutableJacksonDataConverter.builder().build();

    @Test
    public void basicTypesSerializeDeserializeTest() {
        Object[] input = new Integer[] {1, 2, 3};
        input = Arrays.copyOf(input, input.length, Object[].class);
        final String json = dataConverter.toData(input);
        Object[] output = dataConverter.fromData(json, Object[].class);
        assertThat("cant convert basic types", input, is(output));
    }

    @Test
    public void emptyStringDeserializeTest() {
        String input = "";
        final String result = dataConverter.fromData(input, TypeToken.of(String.class).getType());
        assertThat("deserialize empty fine", result, nullValue());
    }
}
