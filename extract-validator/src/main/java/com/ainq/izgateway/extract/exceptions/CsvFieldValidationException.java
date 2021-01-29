package com.ainq.izgateway.extract.exceptions;
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
import com.ainq.izgateway.extract.CVRSExtract;
import com.ainq.izgateway.extract.validation.CVRSEntry;
import com.opencsv.exceptions.CsvValidationException;

public class CsvFieldValidationException extends CsvValidationException {
    private static final long serialVersionUID = 1L;
    private String field;
    private String hl7pos = null;
    private String key;
    private CVRSExtract extract;
    /**
     * Default constructor.
     * @param extract   The CVRSExtract being reported on
     * @param field The field in error
     */
    public CsvFieldValidationException(CVRSExtract extract, String field) {
        this(extract, null, null, field);
    }

    /**
     * Constructor that allows for a human readable message.
     *
     * @param extract   The CVRSExtract being reported on
     * @param key The error code
     * @param message   The error message.
     * @param field The field in error
     */
    public CsvFieldValidationException(CVRSExtract extract, String key, String message, String field) {
        super(message);
        this.extract = extract;
        this.field = field;
        this.key = key;
    }

    /**
     * Get the position of the error
     * @return  the position of the error, either as a line number or position in an HL7 Message.
     */
    public String getErrorPosition() {
        return hl7pos == null ? Long.toString(getLineNumber()) : hl7pos;
    }

    /**
     * Set the position of an error
     * @param hl7pos    The position of the error in an HL7 message
     * @return  this for fluent use.
     */

    public CsvFieldValidationException setErrorPosition(String hl7pos) {
        this.hl7pos = hl7pos;
        return this;
    }

    /**
     * Get the name of thee field in error.
     * @return  The field name
     */
    public String getField() {
        return field;
    }

    /**
     * Get a validation report entry from this Exception.
     *
     * @return A CVRSEntry for this exception.
     */
    public CVRSEntry getValidationEntry() {
        CVRSEntry e = new CVRSEntry(extract, key, hl7pos == null ? field : hl7pos, getMessage());
        e.setLine((int) getLineNumber());
        e.setRow(getLine());
        return e;
    }
}
