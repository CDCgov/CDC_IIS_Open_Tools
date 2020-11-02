package com.ainq.izgateway.extract.validation;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.MustMatchRegexExpression;
import com.opencsv.exceptions.CsvValidationException;

public class Matches extends MustMatchRegexExpression {
    private String param;

    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        // Allow empty strings to validate, since we use "required=true" to validate missing fields.
        if (!StringUtils.isEmpty(value) && !super.isValid(value)) {
            throw Validator.error(null, "DATA005", field.getField().getName(), param, value);
        }
    }
    @Override
    public void setParameterString(String param) {
        this.param = param;
        super.setParameterString(param);
    }
}