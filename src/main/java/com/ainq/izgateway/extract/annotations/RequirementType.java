package com.ainq.izgateway.extract.annotations;

public enum RequirementType {
    /** This field must always have a value */
    REQUIRED,
    /** This field must be sent when known */
    REQUIRED_IF_KNOWN,
    /** Ignore this field for this version (used for new/removed fields) */
    IGNORE,
    /** This field is optional */
    OPTIONAL,
    /** Do not send the field */
    DO_NOT_SEND
}
