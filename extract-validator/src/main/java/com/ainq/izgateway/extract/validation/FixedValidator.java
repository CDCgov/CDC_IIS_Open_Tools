package com.ainq.izgateway.extract.validation;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class FixedValidator extends SuppressibleValidator implements Fixable, StringValidator {
    private String param = "";
    protected String msg = "DATA002";
    @Override
    public boolean isValid(String value) {
        return param.equalsIgnoreCase(value);
    }

    @Override
    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        if (!isValid(value)) {
            throw Validator.error(null, msg, field.getField().getName(), param, value);
        }
    }

    @Override
    public void setParameterString(String value) {
        this.param = value;
    }

    @Override
    public String fixIt(String value) {
        return param;
    }
}