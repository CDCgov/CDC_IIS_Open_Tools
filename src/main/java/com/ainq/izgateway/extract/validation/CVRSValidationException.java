package com.ainq.izgateway.extract.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.CVRSExtract;
import com.opencsv.exceptions.CsvConstraintViolationException;

import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Report;
import gov.nist.validation.report.impl.ReportImpl;

public class CVRSValidationException extends CsvConstraintViolationException {

    private static final long serialVersionUID = 1L;
    List<CVRSEntry> errors;

    public CVRSValidationException(CVRSExtract bean, List<CVRSEntry> errors) {
        super(bean, errors.toString());
        this.errors = errors;
    }

    @Override
    public void setLineNumber(long line) {
        for (CVRSEntry e: errors) {
            e.setLine((int) line);
        }
    }

    public List<CVRSEntry> getEntries() {
        return Collections.unmodifiableList(errors);
    }

    public Report getReport() {
        Map<String, List<Entry>> map = new HashMap<>();
        String prefixes[] = { "DATA", "REQD", "BUS", "DNTS" };
        for (String prefix: prefixes) {
            map.put(prefix,
                Arrays.asList(
                    errors.stream().filter(e -> StringUtils.startsWith(e.getCategory(), prefix)).toArray(Entry[]::new)
                )
            );
        }
        return new ReportImpl(map);
    }

    public String toString() {
        return getReport().toText();
    }
}
