package com.ainq.izgateway.extract.validation;

import com.ainq.izgateway.extract.annotations.ExtractType;

public class RedactedValidator extends FixedValidator implements ExtractTypeBasedValidator {
    private boolean isRedacting = true;
    private ExtractType extractType = ExtractType.REDACTED;

    public RedactedValidator() {
        setParameterString("Redacted");
        msg = "DATA006"; // Same message as DATA002, but different code.
    }

    public boolean isValid(String value) {
        if (isRedacting)
            return super.isValid(value);
        return true;
    }

    public void setParameterString(String value) {
        super.setParameterString("Redacted");
    }

    public RedactedValidator setRedacting(boolean isRedacting) {
        this.isRedacting = isRedacting;
        return this;
    }

    public boolean isRedacting() {
        return isRedacting;
    }

    @Override
    public ExtractType getExtractType() {
        return extractType;
    }

    @Override
    public RedactedValidator setExtractType(ExtractType type) {
        this.extractType = type;
        setRedacting(!ExtractType.IDENTIFIED.equals(type));
        return this;
    }
}