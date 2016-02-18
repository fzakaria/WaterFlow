package com.github.fzakaria.waterflow.converter;

import java.lang.reflect.Type;

/**
 * A light abstraction around various serializing/deserializing libraries.
 */
public interface DataConverter {

    /**
     * Given any input object return the String representation of it
     * @param input The object to serialize
     * @return The serialized string
     * @throws DataConverterException if anything goes awry
     */
    String toData(Object input) throws DataConverterException;

    /**
     * Given a input string and a target type, coerce the string to it
     * @param input The serialized version of the final output
     * @param type The type to be converted to
     * @return The deserialized result
     * @throws DataConverterException if anything goes awry
     */
    <T> T fromData(String input, Type type) throws DataConverterException;

}
