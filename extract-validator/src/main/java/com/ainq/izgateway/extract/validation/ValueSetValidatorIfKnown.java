package com.ainq.izgateway.extract.validation;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.bean.validators.StringValidator;

public class ValueSetValidatorIfKnown extends ValueSetValidator implements Fixable, StringValidator {
    public boolean isValid(String value) {
        if (StringUtils.isEmpty(value))
            return true;
        return super.isValid(value);
    }
}