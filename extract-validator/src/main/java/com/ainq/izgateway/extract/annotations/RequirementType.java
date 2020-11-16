package com.ainq.izgateway.extract.annotations;

public enum RequirementType {
    /** This field must always have a value */
    REQUIRED("REQD"),
    /** This field must be sent when known */
    REQUIRED_IF_KNOWN("RIFK"),
    /** Ignore this field for this version (used for new/removed fields) */
    IGNORE("IGNR"),
    /** This field is optional */
    OPTIONAL("OPT_"),
    /** Do not send the field */
    DO_NOT_SEND("DNTS");
    String code;
    RequirementType(String code) {
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
