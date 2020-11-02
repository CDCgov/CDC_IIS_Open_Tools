package com.ainq.izgateway.extract.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.CVRSExtract;
import com.ainq.izgateway.extract.Validator;
import com.ainq.izgateway.extract.annotations.DoNotSend;
import com.ainq.izgateway.extract.annotations.EventType;
import com.ainq.izgateway.extract.annotations.FieldValidator;
import com.ainq.izgateway.extract.annotations.Required;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.validators.StringValidator;
import com.opencsv.exceptions.CsvFieldValidationException;
import com.opencsv.exceptions.CsvValidationException;

public class BeanValidator extends SuppressibleValidator implements BeanVerifier<CVRSExtract> {
    /** The business rules to execute */
    private static String rules[][] = {
        { "BUSR001", "recip_address_county in recip_address_state FIPS", "%s (%s) does not match %s (%s) for Recipient"},
        { "BUSR002", "admin_address_county in admin_address_state FIPS", "%s (%s) does not match %s (%s) for Vaccine Administrator"},
        { "BUSR003", "recip_address_zip in recip_address_state STATE", "%s (%s) does not match %s (%s) for Recipient"},
        { "BUSR004", "admin_address_zip in admin_address_state STATE", "%s (%s) does not match %s (%s) for Vaccine Administrator" },
        { "BUSR005", "recip_dob < admin_date", "%s (%s) is after %s (%s)" },
        { "BUSR006", "vax_refusal NAND recip_missed_appt", "%s (%s) and %s (%s) should not both be YES" },
        { "BUSR007", "recip_address_zip implies recip_address_state", "If %s (%s) is present, %s (%s) should be present for Recipient"},
        { "BUSR008", "admin_address_zip implies admin_address_state", "If %s (%s) is present, %s (%s) should be present for Vaccine Administrator"},
        { "BUSR009", "recip_address_zip implies recip_address_county", "If %s (%s) is present, %s (%s) should be present for Recipient"},
        { "BUSR010", "admin_address_zip implies admin_address_county", "If %s (%s) is present, %s (%s) should be present for Vaccine Administrator" },
        { "BUSR011", "recip_dob no_time_travel recip_dob", "If %s (%s) is present it should be less than %s (%s)"},
        { "BUSR012", "admin_date no_time_travel admin_date", "If %s (%s) is present it should less than %s (%s)" },
        { "BUSR013", "vax_event_id nodups vax_event_id", "Value of %s (%s) duplicates %s at line %s" }
    };
    /** Map of states to FIP Prefixes to validate county in state */
    private static Map<String, String> stateToCounty = new TreeMap<>();
    /** Map of states to zip-code prefixes to validate zip in state */
    private static Map<String, String> stateToZip = new TreeMap<>();

    private Map<String, String> event_id = new HashMap<>();
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

    public Map<String, String> getEventIds() {
        return event_id;
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
        String values[] = bean.getValues();
        checkRequirements(bean, errors);
        Date d = new Date();
        // This is good enough for testing the rule even though it is both deprecated, and not using good date math.
        String tomorrow = String.format("%04d-%02d-%02d", d.getYear() + 1900, d.getMonth()+1, d.getDate() + 1);
        for (String rule[]: rules) {
            String parts[] = rule[1].split("\\s+");
            String field1 = bean.getField(parts[0]), field2 = bean.getField(parts[2]);
            if (getSuppressed().contains(rule[0])) {
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
                String line = event_id.get(field1);
                success = line == null;
                event_id.put(field1, field2 = Integer.toString(++counter));
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
                if (!StringUtils.isEmpty(field1)) {
                   success = field1.compareTo(field2) <= 0;  // Maybe it could be same day
                }
                break;
            case "XOR":
                success = Validator.isTrue(field1) != Validator.isTrue(field2);
                break;
            case "NAND":
                success = !("YES".equals(field1) && "YES".equals(field2));
                break;
            }
            if (!success) {
                CVRSEntry e = new CVRSEntry(bean, rule[0], parts[0], String.format(rule[2], parts[0], field1, parts[2], field2));
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

    private void checkRequirements(CVRSExtract bean, List<CVRSEntry> errors) {
        int fieldCount = 0;
        String values[] = bean.getValues();
        for (Field f: CVRSExtract.class.getDeclaredFields()) {
            fieldCount++;
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) != 0) {
                // Skip Static and transient fields.
                continue;
            }

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
            Required req = f.getAnnotation(Required.class);

            f.setAccessible(true);
            String value;
            try {
                value = (String)f.get(bean);
            } catch (IllegalArgumentException | IllegalAccessException e1) {
                // SHOULDN'T Happen, we've already done this successfully by now.
                throw new RuntimeException("Error retrieving bean field " + f.getName(), e1);
            }

            if (req != null) {
                EventType[] when = req.versioned() && getVersion().equals("1") ? req.whenv1() : req.when();
                if (Arrays.asList(when).contains(eventType)) {
                    String code = String.format("REQD%03d", eventType.ordinal() + 1);
                    if (!getSuppressed().contains(code) && StringUtils.isEmpty(value)) {
                        CVRSEntry e = new CVRSEntry(bean, code, f.getName(),
                            String.format("%s: %s is required for %s events", code, f.getName(), eventType));
                        e.setRow(values);
                        errors.add(e);
                    }
                }
            }

            DoNotSend dns = f.getAnnotation(DoNotSend.class);
            if (dns != null) {
                EventType[] when = dns.versioned() && getVersion().equals("1") ? dns.whenv1() : dns.when();
                if (Arrays.asList(when).contains(eventType)) {
                    String code = String.format("DNTS%03d", eventType.ordinal() + 1);
                    if (!getSuppressed().contains(code) && !StringUtils.isEmpty(value)) {
                        CVRSEntry e = new CVRSEntry(bean, code, f.getName(), String.format("%s: %s (%s) should not be present for %s events",
                            code, f.getName(), value, eventType));
                        e.setRow(values);
                        errors.add(e);
                    }
                }
            }

            FieldValidator fv = f.getAnnotation(FieldValidator.class);
            ValidatorBeanField vbf = new ValidatorBeanField(f);
            if (fv != null) {
                StringValidator sv;
                try {
                    sv = fv.validator().getConstructor().newInstance();
                    if (sv instanceof Suppressible) {
                        ((Suppressible) sv).setSuppressed(getSuppressed());
                    }
                    if (sv instanceof SuppressibleValidator) {
                        ((SuppressibleValidator) sv).setVersion(getVersion());
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
                    stateToCounty.put(parts[0], parts[2]);
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

        String code = stateToCounty.get(state);
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

        String zips = stateToZip.get(state);
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