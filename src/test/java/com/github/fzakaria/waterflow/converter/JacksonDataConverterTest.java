package com.github.fzakaria.waterflow.converter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JacksonDataConverterTest {

    private final JacksonDataConverter dataConverter = new JacksonDataConverter();

    @Test
    public void basicTypesSerializeDeserializeTest() {
        Object[] input = new Integer[] {1, 2, 3};
        input = Arrays.copyOf(input, input.length, Object[].class);
        final String json = dataConverter.toData(input);
        Object[] output = dataConverter.fromData(json, Object[].class);
        assertThat("cant convert basic types", input, is(output));
    }
}
