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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class DateValidator extends SuppressibleValidator implements Fixable, StringValidator, Suppressible {
    private String param = null;
    private static String DEFAULT_FORMAT = "yyyy-MM-dd|yyyy-MM";
    private SimpleDateFormat sdf[] = { new SimpleDateFormat(DEFAULT_FORMAT) };
    @Override
    public boolean isValid(String value) {
        // Allow empty values to validate, use required=true to force them to be non-empty
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        for (SimpleDateFormat fmt: sdf) {
            try {
                // Wee do this to overcome some leniency in interpretation of months.
                Date d = fmt.parse(value);
                String v = fmt.format(d);
                return v.equalsIgnoreCase(value);
            } catch (ParseException e) {
            }
        }
        return false;
    }

    @Override
    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        if (!isValid(value)) {
            SimpleDateFormat usFormat = new SimpleDateFormat(
                "MM/dd/yyyy".substring(0, Math.min(value.length(), 10)));
            try {
                usFormat.parse(value);
                throw Validator.error(null, "DATA009", field.getField().getName(), value, param);
            } catch (ParseException ex) {
                // Swallow this, it's invalid rather than incorrectly formatted.
            }
            throw Validator.error(null, "DATA001", field.getField().getName(), value, param);
        }
    }

    @Override
    public void setParameterString(String value) {
        param = StringUtils.isEmpty(value) ? DEFAULT_FORMAT : value;
        String fmts[] = param.split("\\|");
        sdf = new SimpleDateFormat[fmts.length];
        int count = 0;
        for (String fmt: fmts) {
            sdf[count] = new SimpleDateFormat(fmt);
            sdf[count].setLenient(false);
            count++;
        }
    }

    @Override
    public String fixIt(String value) {
        String newValue = null;
        if (!StringUtils.isEmpty(value)) {
            value = value.replaceAll("[0-9-/]","");
            if (value.length() == 0) {
                return value;
            }
            String yearPart = value.replaceAll("^.*(\\d{4}).*$", "$1");
            String monthPart = value.replaceAll("^(\\d{2})/.*$|-(\\d{2})-", "$1$2");
            String dayPart = value.replaceAll("^\\d{2}/(\\d{2}).*$|\\d{2}-(\\d{2})[^-]?$", "$1$2");
            if (yearPart.length() == 4 && monthPart.length() == 2 && dayPart.length() == 2) {
                newValue = String.format("%s-%s-%s", yearPart, monthPart, dayPart);
                if (isValid(newValue)) {
                    return newValue;
                }
            }
        }
        // Go with today.
        Calendar cal = Calendar.getInstance();
        newValue = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE));

        if (isValid(newValue)) {
            return newValue;
        }
        return value;
    }
}