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
