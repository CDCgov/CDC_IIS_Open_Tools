package com.ainq.izgateway.extract.validation;

import com.ainq.izgateway.extract.Validator;
import com.ainq.izgateway.extract.annotations.ExtractType;
import com.opencsv.bean.BeanField;
import com.opencsv.exceptions.CsvValidationException;

public class ExtractTypeValidator extends Matches implements ExtractTypeBasedValidator {
    private String param;
    ExtractType type = ExtractType.REDACTED;

    public boolean isValid(String value) {
        return type != null && type.getCode().equalsIgnoreCase(value);
    }

    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        if (!isValid(value)) {
            throw Validator.error(null, "DATA005", field.getField().getName(), param, value);
        }
    }
    @Override
    public void setParameterString(String param) {
        this.param = param;
        super.setParameterString(param);
    }

    public ExtractTypeValidator setExtractType(ExtractType t) {
        type = t;
        return this;
    }

    public ExtractType getExtractType() {
        return type;
    }
}