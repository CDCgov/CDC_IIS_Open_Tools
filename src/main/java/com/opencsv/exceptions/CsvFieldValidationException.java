package com.opencsv.exceptions;

import com.ainq.izgateway.extract.CVRSExtract;
import com.ainq.izgateway.extract.validation.CVRSEntry;

public class CsvFieldValidationException extends CsvValidationException {
    private static final long serialVersionUID = 1L;
    private String field;
    private String hl7pos = null;
    private String key;
    private CVRSExtract extract;
    /**
     * Default constructor.
     */
    public CsvFieldValidationException(CVRSExtract extract, String field) {
        this(extract, null, null, field);
    }

    /**
     * Constructor that allows for a human readable message.
     *
     * @param message - error text.
     */
    public CsvFieldValidationException(CVRSExtract extract, String key, String message, String field) {
        super(message);
        this.extract = extract;
        this.field = field;
        this.key = key;
    }


    public String getErrorPosition() {
        return hl7pos == null ? Long.toString(getLineNumber()) : hl7pos;
    }

    public CsvFieldValidationException setErrorPosition(String hl7pos) {
        this.hl7pos = hl7pos;
        return this;
    }

    public String getField() {
        return field;
    }

    public CVRSEntry getValidationEntry() {
        CVRSEntry e = new CVRSEntry(extract, key, hl7pos == null ? field : hl7pos, getMessage());
        e.setLine((int) getLineNumber());
        e.setRow(getLine());
        return e;
    }
}
