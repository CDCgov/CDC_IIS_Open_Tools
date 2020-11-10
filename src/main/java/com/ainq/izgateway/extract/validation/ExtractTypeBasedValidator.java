package com.ainq.izgateway.extract.validation;

import com.ainq.izgateway.extract.annotations.ExtractType;

/**
 * Interface for validators that change their behavior
 * based on the extract type.
 *
 * @author Keith W. Boone
 *
 */
public interface ExtractTypeBasedValidator {
    /**
     * Get the Extract Type to validate for
     * @return  An ExtractType
     */
    ExtractType getExtractType();
    /**
     * Set the Extract Type to validate for
     * @param The Extract Type to validate
     * @return  this for fluent operation
     */
    ExtractTypeBasedValidator setExtractType(ExtractType type);
}
