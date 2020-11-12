package com.ainq.izgateway.extract.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class DateValidator extends SuppressibleValidator implements StringValidator, Suppressible {
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
}