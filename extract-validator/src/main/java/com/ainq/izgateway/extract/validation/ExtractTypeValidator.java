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