package com.ainq.izgateway.extract.validation;

public class RedactedValidator extends FixedValidator {

    public RedactedValidator() {
        setParameterString("Redacted");
        msg = "DATA006"; // Same message as DATA002, but different code.
    }

    public void setParameterString(String value) {
        super.setParameterString("Redacted");
    }
}