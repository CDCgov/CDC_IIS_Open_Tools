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