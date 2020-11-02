package com.ainq.izgateway.extract.validation;

import java.util.Set;

/**
 * This interface is used to mark a validator as being suppressible,
 * and provides the basic interface to tell the validator what errors
 * to suppress or determine which errors are being suppressed.
 *
 * @author Keith W. Boone
 *
 */
public interface Suppressible {
    /**
     * This method will be called after the validator is constructed
     * before any parameters are set to indicate which errors should
     * be suppressed during validation.  This enables validators to
     * have dynamic behavior based on system configuration.
     *
     * @param suppressed    A list of error codes to be suppressed.
     */
    void setSuppressed(Set<String> suppressed);

    /**
     * This method may be used by a Validator to determine which
     * errors should be suppressed.
     *
     * @return  The list of error codes that should be suppressed.
     */
    Set<String> getSuppressed();
}
