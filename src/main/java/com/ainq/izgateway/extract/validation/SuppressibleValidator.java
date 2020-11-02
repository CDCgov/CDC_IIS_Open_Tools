package com.ainq.izgateway.extract.validation;

import java.util.Collections;
import java.util.Set;

import com.ainq.izgateway.extract.Validator;

/**
 * This is the base class for a Validator that supports
 * the Suppressible interface.
 *
 * @author Keith W. Boone
 *
 */
public class SuppressibleValidator implements Suppressible {
    /** The initial set of validation errors to be suppressed.
     *  Initialized to none.
     */
    private Set<String> suppressed = Collections.emptySet();

    /**
     * The version of the CVRS Format to validate against.
     */
    private String version = Validator.DEFAULT_VERSION;

    @Override
    public void setSuppressed(Set<String> suppressed) {
       this.suppressed = suppressed;
    }

    @Override
    public Set<String> getSuppressed() {
        return suppressed;
    }

    /**
     * Get the version of CVRS to validate against.
     * @return the version of CVRS to validate against.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version of CVRS to validate against.
     * @param version the version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
