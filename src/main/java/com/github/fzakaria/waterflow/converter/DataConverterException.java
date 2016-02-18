package com.github.fzakaria.waterflow.converter;

/**
 * A runtime exception that signifies something unexpected
 * has occurred during data converting.
 */
public class DataConverterException extends RuntimeException {

    public DataConverterException(Throwable cause) {
        super(cause);
    }

}
