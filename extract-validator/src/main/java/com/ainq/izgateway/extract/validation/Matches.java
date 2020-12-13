package com.ainq.izgateway.extract.validation;
/*
 * Copyright 2020 Audiacious Inquiry, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.MustMatchRegexExpression;
import com.opencsv.exceptions.CsvValidationException;

public class Matches extends MustMatchRegexExpression implements Fixable {
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
    @Override
    public String fixIt(String value) {
        if (value == null) {
            value = "";
        }

        if (param.contains("[0-9A-Za-z]")) {
            String v = value.split("\\s")[0];
            v = v.replaceAll("[^0-9A-Za-z]","");
            return v.substring(Math.max(0, v.length() - 6));
        }

        if (param.contains("\\d{5}")) {
            String v = value.replaceAll("\\D", "");
            if (v.length() < 5) {
                v = "00000" + v;
                return v.substring(v.length() - 5);
            }
            if (v.length() >= 5 && v.length() < 9) {
                return v.substring(0, 5);
            }
            return v.substring(0, 5) + "-" + v.substring(5, 9);
        }

        return value.replaceAll("\\s", "");
    }
}