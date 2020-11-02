package com.ainq.izgateway.extract.validation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.CVRSExtract;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvFieldValidationException;

import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Trace;

/**
 * This is an entry used for Error reporting. It uses
 * the same structure as NIST V2 Validation tools.
 *
 */
public class CVRSEntry implements Entry {

    CVRSExtract extract = null;
    private String classification = "ERROR";
    private String category = "";
    private String message = null;
    private int line = -1;
    private int column = -1;
    private String path = null;
    private String row[] = new String[0];

    public CVRSEntry(CVRSExtract extract, String category, String path, String message) {
        this.extract = extract;
        this.category = category;
        this.path = path;
        this.message = message;
    }

    public CVRSEntry(CVRSValidationException ex, int line) {
        this((CVRSExtract)ex.getSourceObject(), ex, line);
    }

    public CVRSEntry(CVRSExtract extract, CsvException err, int line) {
        this.extract = extract;
        message = err.getMessage();
        if (err instanceof CsvFieldValidationException) {
            CVRSEntry e = ((CsvFieldValidationException) err).getValidationEntry();
            line = e.line;
            column = e.column;
            path = e.path;
            row = e.row;
        } else if (err instanceof CVRSValidationException) {
            this.line = line;
            err.setLineNumber(line);
            message = ((CVRSValidationException) err).getReport().toText();
            row = err.getLine();
        } else {
            category = StringUtils.substringBefore(message, ":");
            if (category != null) {
                message = StringUtils.substringAfter(message, ":");
            }
            line = (int) err.getLineNumber();
            path = getFieldName(err);
            row = err.getLine();
        }
    }

    public CVRSEntry(CVRSExtract extract, int line) {
        this.extract = extract;
        this.classification = "INFO";
        this.category = "OK";
        this.row = extract.getValues();
        this.line = line;
    }

    private static String getFieldName(CsvException err) {
        if (err instanceof CsvConstraintViolationException) {
            return null;
        }
        String msg = err.getMessage();
        String fieldName = msg.replaceAll("^.*Field (\"|\')?([^ '\"]*)(\"|\')? .*$", "$2");
        if (fieldName == null || fieldName.length() == 0)
            return null;
        return fieldName;
    }
    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getClassification() {
        return classification;
    }

    @Override
    public int getColumn() {
        return column;
    }

    public CVRSEntry setColumn(int column) {
        this.column = column;
        return this;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return message;
    }

    @Override
    public int getLine() {
        // TODO Auto-generated method stub
        return line;
    }

    public CVRSEntry setLine(int line) {
        this.line = line;
        return this;
    }

    @Override
    public Map<String, Object> getMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return path;
    }

    @Override
    public List<Trace> getStackTrace() {
        return Collections.emptyList();
    }

    @Override
    public String toJson() throws Exception {
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("classification", classification);
        obj.add("category", category);
        obj.add("description", message);
        obj.add("line", line);
        obj.add("column", column);
        obj.add("path", path);
        JsonObject o = obj.build();
        return o.toString();
    }

    @Override
    public String toText() {
        // TODO Auto-generated method stub
        return String.format("%s %d:(%s) %s: %s", classification, line, path, category, message);
    }

    public String[] getRow() {
        return row;
    }

    public CVRSEntry setRow(String[] row) {
        this.row = row;
        return this;
    }

    public String toString() {
        return "\n\t" + toText();
    }

}
