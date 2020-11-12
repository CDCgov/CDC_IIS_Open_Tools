package com.ainq.izgateway.extract.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    private Map<String, Pair<Integer, Integer>> event_id = new HashMap<>();
    private int counter;

    /**
     * Construct a new validator using the default CVRS Version
     *
     * @param suppressed    The errors to suppress.
     */
    public BeanValidator(Set<String> suppressed) {
        this(suppressed == null ? Collections.emptySet() : suppressed, Validator.DEFAULT_VERSION);
    }

    /**
     * Construct a new validator using the specified CVRS Version
     *
     * @param suppressed    The errors to suppress.
     * @param version   The version of the CVRS to validate against.
     */
    public BeanValidator(Set<String> suppressed, String version) {
        setSuppressed(suppressed == null ? Collections.emptySet() : suppressed);
        setVersion(version);
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

    private void checkRequirements(CVRSExtract bean, List<CVRSEntry> errors) {
        String values[] = bean.getValues();
        EventType eventType = getEventType(bean);
        ExtractType extractType = getExtractType(bean);

        for (Field f: CVRSExtract.class.getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) != 0) {
                // Skip Static and transient fields.
                continue;
            }

            f.setAccessible(true);
            String value;
            try {
                // If the field isn't used in this version of the CVRS
                // Set the field to null in the extracted bean.
                if (getRequirement(f, RequirementType.IGNORE, getVersion()) != null) {
                    f.set(bean, null);
                }
                // Get the value.
                value = (String)f.get(bean);
            } catch (IllegalArgumentException | IllegalAccessException e1) {
                // SHOULDN'T Happen, we've already done this successfully by now.
                throw new RuntimeException("Error retrieving bean field: " + f.getName(), e1);
            }

            Requirement req = getRequirement(f, RequirementType.REQUIRED, getVersion());
            if (req != null && Arrays.asList(req.when()).contains(eventType)) {
                String code = String.format("REQD%03d", eventType.ordinal() + 1);
                if (!getSuppressed().contains(code) && StringUtils.isEmpty(value)) {
                    CVRSEntry e = new CVRSEntry(bean, code, f.getName(),
                        String.format("%s is required for %s events", f.getName(), eventType));
                    e.setRow(values);
                    errors.add(e);
                }
            }

            Requirement dns = getRequirement(f, RequirementType.DO_NOT_SEND, getVersion());
            if (dns != null && Arrays.asList(dns.when()).contains(eventType)) {
                String code = String.format("DNTS%03d", eventType.ordinal() + 1);
                if (!getSuppressed().contains(code) && !StringUtils.isEmpty(value)) {
                    CVRSEntry e = new CVRSEntry(bean, code, f.getName(), String.format("%s (%s) should not be present for %s events",
                        f.getName(), value, eventType));
                    e.setRow(values);
                    errors.add(e);
                }
            }

            FieldValidator fv = f.getAnnotation(FieldValidator.class);
            ValidatorBeanField vbf = new ValidatorBeanField(f);
            if (fv != null) {
                int length = value == null ? 0 : value.length();
                if (length > fv.maxLength()) {
                    CVRSEntry e1 = new CVRSEntry(bean, "DATA008", f.getName(), Validator.getMessage("DATA008", f.getName(), value, fv.maxLength()));
                    errors.add(e1);
                }
                StringValidator sv;
                try {
                    sv = fv.validator().getConstructor().newInstance();
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
                    sv.validate(value, vbf);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    throw new RuntimeException("Unable to construct " + fv.validator().getName(), e);
                } catch (CsvValidationException e) {
                    if (e instanceof CsvFieldValidationException) {
                        errors.add(((CsvFieldValidationException) e).getValidationEntry());
                    } else {
                        String message = e.getMessage();
                        String code = StringUtils.substringBefore(message, ":");
                        if (!getSuppressed().contains(code)) {
                            CVRSEntry e1 = new CVRSEntry(bean, code, null, StringUtils.substringAfter(message, ":"));
                            e1.setRow(values);
                            errors.add(e1);
                        }
                    }
                }
            }
        }
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