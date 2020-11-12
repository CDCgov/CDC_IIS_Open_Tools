package com.ainq.izgateway.extract.validation;

public interface Fixable {
    /**
     * Correct the supplied value
     * @param value The value to correct.
     * @return A corrected value.
     */
    String fixIt(String value);
}
