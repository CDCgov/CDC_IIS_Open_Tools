package com.ainq.izgateway.extract.validation;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class DoNotPopulateValidator extends SuppressibleValidator implements StringValidator {
    @Override
    public boolean isValid(String value) {
        return value == null || value.isEmpty();
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

}