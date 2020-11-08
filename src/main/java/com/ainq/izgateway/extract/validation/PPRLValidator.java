package com.ainq.izgateway.extract.validation;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.ainq.izgateway.extract.annotations.ExtractType;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class PPRLValidator extends SuppressibleValidator implements StringValidator, ExtractTypeBasedValidator {
    boolean isPPRL = false;
    private ExtractType extractType = ExtractType.REDACTED;
    @Override
    public boolean isValid(String value) {
        return isPPRL == !StringUtils.isEmpty(value);
    }

    @Override
    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        if (!isValid(value)) {
            throw Validator.error(null, "DATA004", field.getField().getName(), value);
        }
    }

    @Override
    public void setParameterString(String value) {
    }

    public boolean isPPRL() {
        return isPPRL;
    }

    public PPRLValidator setPPRL(boolean isPPRL) {
        this.isPPRL = isPPRL;
        return this;
    }

    @Override
    public ExtractType getExtractType() {
        return extractType;
    }

    @Override
    public PPRLValidator setExtractType(ExtractType type) {
        extractType = type;
        setPPRL(ExtractType.PPRL.equals(type));
        return this;
    }
}