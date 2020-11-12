package com.ainq.izgateway.extract.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.Validator;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvValidationException;

public class ValueSetValidator extends SuppressibleValidator implements StringValidator {
    private String valueSetName;
    private String activeValueSet;
    private Set<String> values = new TreeSet<String>();
    private String examples = "";
    private static Map<String, Set<String>> valueSets = new HashMap<>();
    private static Map<String, String> exampleMap = new HashMap<>();

    @Override
    public boolean isValid(String value) {
        // Allow empty values to validate, use required=true to force values
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        ensureValuesLoaded();
        return values.contains(value.trim().toUpperCase());
    }

    @Override
    public void validate(String value, @SuppressWarnings("rawtypes") BeanField field) throws CsvValidationException {
        if (!isValid(value)) {
            throw Validator.error(null, "DATA007", field.getField().getName(), activeValueSet, examples, value);
        }
    }

    @Override
    public void setParameterString(String value) {
        this.valueSetName = value;
        values.clear();
    }

    private void ensureValuesLoaded() {
        if (!values.isEmpty()) {
            return;
        }
        activeValueSet = valueSetName;
        if (valueSetName.contains("=")) {
            String parts[] = valueSetName.split("\\|");
            String lookingFor = getVersion() + "=";
            for (String part: parts) {
                if (part.startsWith(lookingFor) || part.startsWith("*=")) {
                    activeValueSet = part.substring(lookingFor.length());
                    break;
                }
            }
        }
        Set<String> loaded = valueSets.get(activeValueSet);
        if (loaded != null) {
            values.addAll(loaded);
            examples = exampleMap.get(activeValueSet);
            return;
        }

        InputStream s = getClass().getClassLoader().getResourceAsStream(activeValueSet + ".txt");
        if (s == null) {
            MissingResourceException ex = new MissingResourceException("Cannot access value set: " + activeValueSet, this.getClass().getCanonicalName(), activeValueSet);
            throw ex;
        }
        try (
            BufferedReader r = new BufferedReader(new InputStreamReader(s));
        ) {
            String line = null;
            List<String> examples = new ArrayList<>();
            boolean elide = false;
            while ( (line = r.readLine()) != null) {
                String parts[] = line.trim().split("\\s+");
                if (parts[0].length() != 0) {
                    values.add(parts[0].toUpperCase());
                    switch (examples.size()) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        examples.add(parts[0]);
                        break;
                    case 4:
                        elide = true;
                        examples.remove(2);
                        examples.add(parts[0]);
                        break;
                    }
                }
            }
            valueSets.put(activeValueSet, new TreeSet<>(values));
            this.examples = !elide ? examples.toString() :
                String.format("[%s, %s ... %s, %s]", examples.get(0), examples.get(1), examples.get(2), examples.get(3));
            exampleMap.put(activeValueSet, this.examples);
        } catch (IOException e) {
            MissingResourceException ex = new MissingResourceException("Error reading value set: " + activeValueSet, this.getClass().getCanonicalName(), activeValueSet);
            ex.initCause(e);
            throw ex;
        }
    }
}