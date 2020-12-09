package com.ainq.izgateway.extract.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.ainq.izgateway.extract.CVRSExtract;
import com.ainq.izgateway.extract.Utility;
import com.ainq.izgateway.extract.Validator;
import com.ainq.izgateway.extract.annotations.EventType;
import com.ainq.izgateway.extract.annotations.ExtractType;
import com.ainq.izgateway.extract.annotations.FieldValidator;
import com.ainq.izgateway.extract.annotations.Requirement;
import com.ainq.izgateway.extract.annotations.RequirementType;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.validators.StringValidator;
import com.ainq.izgateway.extract.exceptions.CsvFieldValidationException;
import com.opencsv.exceptions.CsvValidationException;

public class BeanValidator extends SuppressibleValidator implements BeanVerifier<CVRSExtract> {
    /** The business rules to execute
     *  This array is structured as follows:
     *  Error Code, Rule, Message, Error Level, Version
     *
     *  If Error Level is null or unspecified, then it's an ERROR.
     *  If Version is null or unspecified, then it applies to all versions.
     *
     *  FWIW: This array is in the form needed for a ListMessageBundle
     */
    private static String rules[][] = {
        { "BUSR001", "%s (%s) does not match %s (%s) for Recipient", "recip_address_county in recip_address_state FIPS" },
        { "BUSR002", "%s (%s) does not match %s (%s) for Vaccine Administrator", "admin_address_county in admin_address_state FIPS" },
        { "BUSR003", "%s (%s) does not match %s (%s) for Recipient", "recip_address_zip in recip_address_state STATE" },
        { "BUSR004", "%s (%s) does not match %s (%s) for Vaccine Administrator", "admin_address_zip in admin_address_state STATE"},
        { "BUSR005", "%s (%s) is after %s (%s)", "recip_dob < admin_date"},
        { "BUSR006", "%s (%s) and %s (%s) should not both be YES", "vax_refusal NAND recip_missed_appt", null, "1"},
        { "BUSR007", "If %s (%s) is present, %s (%s) should be present for Recipient", "recip_address_zip implies recip_address_state", "WARN"},
        { "BUSR008", "If %s (%s) is present, %s (%s) should be present for Vaccine Administrator", "admin_address_zip implies admin_address_state", "WARN"},
        { "BUSR009", "If %s (%s) is present, %s (%s) should be present for Recipient", "recip_address_zip implies recip_address_county", "WARN"},
        { "BUSR010", "If %s (%s) is present, %s (%s) should be present for Vaccine Administrator", "admin_address_zip implies admin_address_county", "WARN" },
        { "BUSR011", "If %s (%s) is present it should be less than %s (%s)", "recip_dob no_time_travel"},
        { "BUSR012", "If %s (%s) is present it should less than %s (%s)", "admin_date no_time_travel" },
        { "BUSR013", "Value of %s (%s) duplicates %s at line %s", "vax_event_id nodups" }
    };

    private static final int ERROR_CODE = 0, MESSAGE = 1, RULE = 2, LEVEL = 3, VERSIONS = 4;
    private static final String DEFAULT_LEVEL = "ERROR";
    private static final String DEFAULT_VERSIONS = "1,2"; // Applies to all versions.
    /** Map of states to FIP Prefixes to validate county in state */
    private static Map<String, String> stateToCounty = new TreeMap<>();
    /** Map of states to zip-code prefixes to validate zip in state */
    private static Map<String, String> stateToZip = new TreeMap<>();

    /** A cache of Reusable StringValidator objects that can be called to
     * validate a field.
     */
    private Map<String, StringValidator> fieldValidatorCache = new HashMap<>();

    /** A cache of Reusable ValidatorBeanField objects for calling the validate
     *  method of StringValidator
     */
    private static Map<String, ValidatorBeanField> validatorBeanFields = new HashMap<>();

    private Map<String, Pair<Integer, Integer>> event_id = new HashMap<>();
    private int counter;

    private int fieldCounter[];

    private boolean fixIt;

    /**
     * Construct a new validator using the default CVRS Version
     *
     * @param suppressed    The errors to suppress.
     */
    public BeanValidator(Set<String> suppressed) {
        this(suppressed == null ? Collections.emptySet() : suppressed, Validator.DEFAULT_VERSION, false);
    }

    /**
     * Construct a new validator using the specified CVRS Version
     *
     * @param suppressed    The errors to suppress.
     * @param version   The version of the CVRS to validate against.
     */
    public BeanValidator(Set<String> suppressed, String version) {
        this(suppressed, version, false);
    }

    /**
     * Construct a new validator using the specified CVRS Version
     *
     * @param suppressed    The errors to suppress.
     * @param version   The version of the CVRS to validate against.
     * @param fixIt If an attempt should be made to correct the data
     */
    public BeanValidator(Set<String> suppressed, String version, boolean fixIt) {
        setSuppressed(suppressed == null ? Collections.emptySet() : suppressed);
        setVersion(version);
        this.fixIt = fixIt;
        fieldCounter = new int[CVRSExtract.getHeaders(null).length + 2];
    }

    /**
     * Reset the state of the event_id table for duplicate checking.
     * Allows a BeanValidator to be reused.
     */
    public void resetEventIds() {
        event_id.clear();
    }

    /**
     * Add a previously occuring event to the list of records this validator
     * should reject as a duplicate.
     *
     * @param eventId   The event identifier
     * @param hashCode  The hash code of the original data
     * @param line  The line where this data was defined
     * @return  The old event id replaced by this new one (i.e., the duplicated event)
     */
    public Pair<Integer, Integer> addEventId(String eventId, int hashCode, int line) {
        Pair<Integer, Integer> definingEvent = Pair.of(hashCode, line);
        return event_id.put(eventId, definingEvent);
    }

    public static String getRule(String code) {
        for (String rule[]: rules) {
            if (rule[ERROR_CODE].equals(code)) {
                return rule[RULE];
            }
        }
        return null;
    }

    public int getCounter() {
        return counter;
    }

    public BeanValidator setCounter(int counter) {
        this.counter = counter;
        return this;
    }

    @Override
    public boolean verifyBean(CVRSExtract bean) throws CVRSValidationException {
        List<CVRSEntry> errors = new ArrayList<>();
        // Increment the validation counter (for duplicate record checking)
        counter++;
        String values[] = bean.getValues();

        checkRequirements(bean, errors);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String tomorrow = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE));
        for (String rule[]: rules) {
            String level = rule.length > LEVEL ? rule[LEVEL] : DEFAULT_LEVEL;
            String versions[] = (rule.length > VERSIONS ? rule[VERSIONS] : DEFAULT_VERSIONS).split(",");
            String parts[] = rule[RULE].split("\\s+");
            String field1 = bean.getField(parts[0]), field2 = parts.length > 2 ? bean.getField(parts[2]) : null;
            // Extend array if needed.
            if (parts.length < 4) {
                parts = Arrays.copyOf(parts, 4);
            }
            if (!Arrays.asList(versions).contains(getVersion())) {
                // Don't apply rules that aren't applicable to the current version
                continue;
            }
            if (getSuppressed().contains(rule[ERROR_CODE])) {
                // Don't check for suppressed errors.
                continue;
            }

            boolean success = false;
            switch (parts[1]) {
            case "in":
                switch (parts[3]) {
                case "FIPS":
                    success = isCountyInState(field1, field2);
                    break;
                case "STATE":
                    success = isZipInState(field1, field2);
                }
                break;
            case "nodups":
                String f1 = StringUtils.defaultString(field1);
                Pair<Integer, Integer> definingEvent = addEventId(
                    f1.toUpperCase(), bean.hashCode(), counter);
                success = definingEvent == null || definingEvent.hashCode() == bean.hashCode();
                parts[2] = parts[0];
                field2 = Integer.toString(definingEvent == null ? event_id.get(f1.toUpperCase()).getValue() : definingEvent.getValue());
                break;
            case "no_time_travel":
                success = StringUtils.isEmpty(field1) || field1.compareTo(tomorrow) < 0;
                field2 = tomorrow;
                parts[2] = "tomorrow";
                break;
            case "implies":
                success = StringUtils.isEmpty(field1) || !StringUtils.isEmpty(field2);
                break;
            case "<":
                if (!StringUtils.isEmpty(field1) && !StringUtils.isEmpty(field2)) {
                   success = field1.compareTo(field2) <= 0;  // Maybe it could be same day
                }
                break;
            case "XOR":
                success = Utility.isTrue(field1) != Utility.isTrue(field2);
                break;
            case "NAND":
                success = !("YES".equalsIgnoreCase(field1) && "YES".equalsIgnoreCase(field2));
                break;
            }
            if (!success) {
                CVRSEntry e = new CVRSEntry(bean, rule[ERROR_CODE], parts[0], String.format(rule[MESSAGE], parts[0], field1, parts[2], field2));
                e.setClassification(level);
                e.setRow(values);
                errors.add(e);
            }
        }
        if (errors.size() != 0) {
            CVRSValidationException ex = new CVRSValidationException(bean, errors);
            ex.setLine(values);
            throw ex;
        }
        return true;
    }


    public static List<Field> getIgnoredFields(String version) {
        List<Field> fields = new ArrayList<>();
        for (Field f: CVRSExtract.class.getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) != 0) {
                // Skip Static and transient fields.
                continue;
            }
            if (getRequirement(f, RequirementType.IGNORE, version) != null) {
                fields.add(f);
            }
        }
        return fields;
    }

    private static String getField(CVRSExtract bean, Field field) {
        try {
            return (String) field.get(bean);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Error retrieving bean field: " + field.getName(), e);
        }
    }

    private static void setField(CVRSExtract bean, Field field, String value) {
        try {
            field.set(bean, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Error Setting bean field: " + field.getName() + " to '" + value + "'", e);
        }
    }

    /**
     * Return a map indicating the names of the fields and the number of
     * times each one has a value.
     *
     * @return A map of field names to field counts.
     */
    public Map<String, Integer> getFieldCounts() {
        int fieldNo = -1;
        TreeMap<String, Integer> map = new TreeMap<>();
        for (Field f: CVRSExtract.class.getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) != 0) {
                // Skip Static and transient fields.
                continue;
            }
            fieldNo++;
            map.put(f.getName(), fieldCounter[fieldNo]);
            switch (f.getName()) {
            case "recip_address_zip":
                map.put(f.getName() + "00", fieldCounter[fieldCounter.length - 2]);
                break;
            case "admin_address_zip":
                map.put(f.getName() + "00", fieldCounter[fieldCounter.length - 1]);
                break;
            }
        }
        return map;
    }

    private void checkRequirements(CVRSExtract bean, List<CVRSEntry> errors) {
        String values[] = bean.getValues();
        EventType eventType = getEventType(bean);
        ExtractType extractType = getExtractType(bean);
        int fieldNo = -1;

        for (Field f: CVRSExtract.class.getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) != 0) {
                // Skip Static and transient fields.
                continue;
            }
            fieldNo++;
            f.setAccessible(true);
            String value;

            // If the field isn't used in this version of the CVRS
            // Set the field to null in the extracted bean.
            if (getRequirement(f, RequirementType.IGNORE, getVersion()) != null) {
                setField(bean, f, null);
            }
            // Get the value.
            value = getField(bean, f);
            if (!StringUtils.isEmpty(value)) {
                fieldCounter[fieldNo]++;
                if (f.getName().contains("_zip") && StringUtils.substring(value, 3).startsWith("00")) {
                    switch (f.getName()) {
                    case "recip_address_zip":
                        fieldCounter[fieldCounter.length - 2] ++;
                        break;
                    case "admin_address_zip":
                        fieldCounter[fieldCounter.length - 1] ++;
                        break;
                    }
                }
            }

            if (StringUtils.isEmpty(value)) {
                String code = hasRequiredEvent(f, RequirementType.REQUIRED, getVersion(), eventType);
                checkRequirement("UNK", bean, errors, values, f, value, code);
            }

            if (!StringUtils.isEmpty(value)) {
                String code = hasRequiredEvent(f, RequirementType.DO_NOT_SEND, getVersion(), eventType);
                checkRequirement(null, bean, errors, values, f, value, code);
            }

            FieldValidator fv = f.getAnnotation(FieldValidator.class);
            if (fv != null) {
                if (StringUtils.length(value) > fv.maxLength()) {
                    checkRequirement(value.substring(0, fv.maxLength()), bean, errors, values, f, value, "DATA008");
                }
                StringValidator sv = getFieldValidator(f.getName(), extractType, fv);
                validateAndFix(bean, f, sv, value, errors, values);
            }
        }
    }

    private void checkRequirement(String newValue, CVRSExtract bean, List<CVRSEntry> errors, String[] values, Field f, String value, String code) {
        if (code != null && !getSuppressed().contains(code)) {
            if (fixIt) {
                setField(bean, f, newValue);
            } else {
                addUnsuppressedError(errors, values, bean, f, code, Validator.getMessage(code, f.getName(), value, StringUtils.length(newValue)));
            }
        }
    }

    /**
     * Given an error condition specified by code, if it hasn't been suppressed, then
     * create a new error entry, and save it in errors,
     * @param errors    The place to save errors
     * @param values    The values in the current bean
     * @param bean      The current bean
     * @param f         The current field
     * @param code      The error code
     * @param message   The message to go with the error
     */
    private void addUnsuppressedError(List<CVRSEntry> errors, String[] values, CVRSExtract bean, Field f, String code, String message) {
        if (!getSuppressed().contains(code)) {
            CVRSEntry e1 = new CVRSEntry(bean, code, f.getName(), message);
            e1.setRow(values);
            errors.add(e1);
        }
    }

    /**
     * Get a ValidatorBeanField for a CVRS record, reusing an existing one
     * where it exists, or creating a new one if it does not.
     *
     * @param f The field to get the ValidatorBeanField for.
     * @return  The new or reused ValidatorBeanField
     */
    private static ValidatorBeanField getValidatorBeanField(Field f) {
        ValidatorBeanField vbf = validatorBeanFields.get(f.getName());
        if (vbf == null) {
            vbf = new ValidatorBeanField(f);
            validatorBeanFields.put(f.getName(), vbf);
        }
        return vbf;
    }

    /**
     * Validate a value, and if fixing errors, retrying after fixing the
     * value, reporting the first error found in retrying fails.
     * @param bean  The bean being validated
     * @param f The field in the bean
     * @param sv    The StringValidator used to validate it
     * @param value The value to validate
     * @param errors    A place to hold errors
     * @param values    The values for the bean
     */
    private void validateAndFix(CVRSExtract bean, Field f, StringValidator sv, String value, List<CVRSEntry> errors, String[] values) {
        ValidatorBeanField vbf = getValidatorBeanField(f);
        try {
            sv.validate(value, vbf);
            return;
        } catch (CsvValidationException e) {
            if (fixIt && sv instanceof Fixable) {
                String newValue = ((Fixable) sv).fixIt(value);
                if (sv.isValid(newValue)) {
                    setField(bean, f, newValue);
                    return;
                }
            }

            if (e instanceof CsvFieldValidationException) {
                errors.add(((CsvFieldValidationException) e).getValidationEntry());
            } else {
                addUnsuppressedError(errors, values, bean, f,
                    StringUtils.substringBefore(e.getMessage(), ":"),
                    StringUtils.substringAfter(e.getMessage(), ":"));
            }
        }
    }
    /**
     * Retrieve the field validator for the specified field
     * @param fieldName   The field to retrieve the validator for.
     * @param extractType   The type of extract being validated.
     * @param fv    The field validator to use
     * @return  An initialized field validator
     * @throws ReflectiveOperationException   If a reflection error occurred.
     */
    private StringValidator getFieldValidator(String fieldName, ExtractType extractType, FieldValidator fv) {
        String key = fieldName + "_" + (extractType == null ? null : extractType.getCode());
        StringValidator sv = fieldValidatorCache.get(key);
        if (sv == null) {
            try {
                sv = fv.validator().getConstructor().newInstance();
            } catch (ReflectiveOperationException | SecurityException e) {
                throw new RuntimeException("Unable to construct " + fv.validator().getName(), e);
            }
            if (sv instanceof Suppressible) {
                ((Suppressible) sv).setSuppressed(getSuppressed());
            }
            if (sv instanceof SuppressibleValidator) {
                ((SuppressibleValidator) sv).setVersion(getVersion());
            }
            if (sv instanceof ExtractTypeBasedValidator) {
                ((ExtractTypeBasedValidator) sv).setExtractType(extractType);
            }
            if (fv.paramString() != null) {
                sv.setParameterString(fv.paramString());
            }
            fieldValidatorCache.put(key, sv);
        }
        return sv;
    }

    public static Requirement getRequirement(Field f, RequirementType type, String version) {
        Requirement r[] = f.getAnnotationsByType(Requirement.class);
        for (Requirement req: r) {
            if (req.value() == type && Arrays.asList(req.versions()).contains(version)) {
                return req;
            }
        }
        return null;
    }

    public static String hasRequiredEvent(Field f, RequirementType type, String version, EventType eventType) {
        Requirement req = getRequirement(f, type, version);
        if (req == null) {
            return null;
        }
        if (Arrays.asList(req.when()).contains(eventType)) {
            return String.format("%s%03d", type.getCode(), eventType.ordinal() + 1);
        }
        return null;
    }

    public static EventType getEventType(CVRSExtract bean) {
        EventType eventType = EventType.VACCINATION;
        if ("YES".equalsIgnoreCase(bean.getRecip_missed_appt())) {
            eventType = EventType.MISSED_APPOINTMENT;
        }
        if ("YES".equalsIgnoreCase(bean.getVax_refusal())) {
            if (eventType == EventType.MISSED_APPOINTMENT) {
                // This error is already reported, we don't need to report it again. Treat as a Refusal.
            }
            eventType = EventType.REFUSAL;
        }
        return eventType;
    }

    /**
     * Examine an extract and see what type is should have.
     * @param bean  The bean to examine
     * @return  The type of extract it represents.
     */
    public static ExtractType getExtractType(CVRSExtract bean) {
        String extractType = bean.getExt_type();
        // If the bean asserts an extract type, it knows best.
        if (!StringUtils.isEmpty(extractType)) {
            for (ExtractType t: ExtractType.values()) {
                if (t.getCode().equalsIgnoreCase(extractType)) {
                    return t;
                }
            }
            return null;
        }
        if (bean.isRedacted()) {
            if (StringUtils.isEmpty(bean.getPprl_id())) {
                return ExtractType.REDACTED;
            }
            return ExtractType.PPRL;
        } else {
            return ExtractType.IDENTIFIED;
        }
    }

    private static synchronized void verifyStatesLoaded() {
        if (stateToZip.isEmpty()) {
            try (BufferedReader r = new BufferedReader(
                new InputStreamReader(CVRSExtract.class.getClassLoader().getResourceAsStream("STATE.txt")));
            ) {
                String line;
                while ((line = r.readLine()) != null) {
                    String parts[] = line.split("\\t");
                    if (parts.length > 3) {
                        stateToZip.put(parts[0], parts[3]);
                    }
                    if (parts.length > 2) {
                        stateToCounty.put(parts[0], parts[2]);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot read STATE.txt Resource");
            }
        }
    }

    public static boolean isCountyInState(String county, String state) {
        if (StringUtils.isEmpty(county) || StringUtils.isEmpty(state)) {
            return true;
        }

        verifyStatesLoaded();

        String code = stateToCounty.get(state.toUpperCase());
        if (StringUtils.isEmpty(code)) {
            // we don't have a county code, so we cannot tell.
            return true;
        }
        // The county code starts with the cooresponding state FIPS code
        return county.startsWith(code);
    }

    public static boolean isZipInState(String zip, String state) {
        if (StringUtils.isEmpty(zip) || StringUtils.isEmpty(state)) {
            return true;
        }

        verifyStatesLoaded();

        String zips = stateToZip.get(state.toUpperCase());
        if (zips == null) {
            return false;
        }
        String zipRanges[] = zips.split(",");
        for (String range: zipRanges) {
            // Range is of form \d\d\d, just check that the zip code first digits match
            if (!range.contains("-") && zip.startsWith(range)) {
                return true;
            }
            String low = StringUtils.substringBefore(range, "-"), high = StringUtils.substringAfter(range, "-");
            // range is of the form low-high.  Verify that zip starts with digits in low or high, or is between low and high
            if (zip.startsWith(low) || zip.startsWith(high) || (zip.compareTo(low) > 0 && zip.compareTo(high) < 0)) {
                return true;
            }
        }

        return false;
    }

}