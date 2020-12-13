package com.ainq.izgateway.extract.annotations;
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
public enum RequirementType {
    /** This field must always have a value */
    REQUIRED("REQD"),
    /** This field must be sent when known */
    REQUIRED_IF_KNOWN("RIFK"),
    /** Ignore this field for this version (used for new/removed fields) */
    IGNORE("IGNR"),
    /** This field is optional */
    OPTIONAL("OPT_"),
    /** Do not send the field */
    DO_NOT_SEND("DNTS");
    String code;
    RequirementType(String code) {
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
