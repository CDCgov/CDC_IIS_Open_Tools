package com.ainq.izgateway.extract;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSEntry;
import com.ainq.izgateway.extract.validation.CVRSValidationException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvFieldValidationException;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
/**
 * A Validator for the CDC Extract File Format
 * @param args
 */
public class Validator implements Closeable {
    /** Bundle for Error messages */
    public static class MyResources extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                { "DATA001", "%1$s (%2$s) contains an invalid date, should match %3$s" },
                { "DATA002", "%1$s (%3$s) should contain value %2$s" },
                { "DATA003", "%1$s (%3$s) does not contain expected value from %2$s" },
                { "DATA004", "%1$s (%2$s) should not be present" },
                { "DATA005", "%1$s (%3$s) does not match the regular expression %2$s" },
                { "DATA006", "%1$s (%3$s) should contain value %2$s" },
                { "DATA007", "%1$s (%4$s) is not in value set %2$s%3$s" },
            };
        };
    }
    /** Display options for validation */
    public enum Show {
        /** Show neither HL7 nor tab-delimited data in standard out */
        NEITHER,
        /** Show only HL7 formatted data in standard out */
        HL7,
        /** Show only CVRS tab-delimited formatted data in standard out */
        CVRS,
        /** Show both HL7 and CVRS tab-delimited formatted data in standard out */
        BOTH
    }
    /** Default version of CVRS to use */
    public static final String DEFAULT_VERSION = "2";
    /** Default max number of errors to allow */
    public static int DEFAULT_MAX_ERRORS = 1000;

    /** Fixed HAPI Context to use for HL7 parsing */
    private static HapiContext context = new DefaultHapiContext();
    /** Fixed HAPI Parser to use for HL7 parsing */
    private static Parser p = context.getGenericParser();

    /** Message bundle for reporting data validation errors */
    private static ResourceBundle MESSAGES = new MyResources();
    /**
     * Helper method to generate a validation exception with a formatted message.
     * @param key   The Error Code (Message code) for the error
     * @param args  Arguments for message formatting.
     * @return  The exception.
     */
    public static CsvFieldValidationException error(CVRSExtract extract, String key, Object ... args) {
        return new CsvFieldValidationException(extract, key, getMessage(key, args), (String)args[0]);
    }
    public static boolean isTrue(String value) {
        // True implies the value is not empty and the value is NOT = NO.
        return !StringUtils.isEmpty(value) && !"NO".equalsIgnoreCase(value);
    }

    public static void main(String args[]) throws IOException {
        int maxErrors = DEFAULT_MAX_ERRORS;
        File cwd = new File(".");
        System.out.println(cwd.getCanonicalPath());
        Show convert = Show.BOTH;
        Set<String> suppressErrors = new TreeSet<>();
        String version = "2";

        // If skip = true, only skip options will be processed and other arguments will be
        // skipped.  This is handy for quick debugging with script files or in Eclipse.
        // Can also be used to include Komments in parameters
        boolean skip = false;
        for (String arg: args) {
            if (arg.startsWith("-k")) {
                skip = true;
                continue;
            }
            if (arg.startsWith("-K")) {
                skip = false;
                continue;
            }
            if (skip) {
                continue;
            }
            if (arg.startsWith("-v")) {
                version = arg.substring(2);
                continue;
            }
            if (arg.startsWith("-e")) {
                // Exit processing: Another handy tool for quick debugging in script files or Eclipse.
                break;
            }
            if (arg.startsWith("-m")) {
                // Set max number of errors before rejecting the file.
                maxErrors = Integer.parseInt(arg.substring(2));
                continue;
            }
            if (arg.startsWith("-c")) {
                convert = Show.CVRS;
                continue;
            }
            if (arg.startsWith("-n")) {
                convert = Show.NEITHER;
                continue;
            }
            if (arg.startsWith("-7")) {
                convert = Show.HL7;
                continue;
            }
            if (arg.startsWith("-b")) {
                convert = Show.BOTH;
                continue;
            }
            if (arg.startsWith("-s")) {
                // Suppress specific errors
                suppressErrors.addAll(Arrays.asList(StringUtils.substring(arg, 2).split("[:,; ]+")));
                continue;
            }
            if (arg.startsWith("-S")) {
                if (arg.length() > 2) {
                    // Turn off specific suppressions.
                    suppressErrors.removeAll(Arrays.asList(StringUtils.substring(arg, 2).split("[:,; ]+")));
                } else {
                    // Turn off all suppressions.
                    suppressErrors.clear();
                }
                continue;
            }

            validateFile(maxErrors, convert, suppressErrors, version, arg);
        }
    }
    private static int getColumnWidth(String[] headers, String[] cols, int i) {
        int width = 0;
        if (i < headers.length) {
            width = headers[i].length();
        }
        if (i < cols.length) {
            width = Math.max(width, cols[i].length());
        }
        return Math.max(4, width + 1);
    }

    /** Helper method to get a formatted message
     *
     * @param key   Message Code (Error Code)
     * @param args  Arguments to generate the message
     * @return The formatted message.
     */
    private static String getMessage(String key, Object ... args) {
        return String.format(MESSAGES.getString(key), args);
    }

    private static void printRow(String[] headers, String[] cols, String header, PrintStream err) {
        StringBuffer l1 = new StringBuffer(), l2 = new StringBuffer();
        int headerPos = headers.length;
        if (header != null) {
            for (headerPos = 0; headerPos < headers.length; headerPos++) {
                if (header.equals(headers[headerPos])) {
                    break;
                }
            }
        }
        for (int i = 0; i < Math.max(headers.length, cols.length); i++) {
            int width = getColumnWidth(headers, cols, i);
            String format = "%-" + width + "s";
            if (headerPos == headers.length || i >= headerPos - 2 && i <= headerPos + 2) {
                l1.append(String.format(format, i < headers.length ? headers[i] : ""));
                l2.append(String.format(format, i < cols.length ? cols[i] : ""));
            }
        }
        err.println(l1.toString());
        err.println(l2.toString());
        err.println();
    }

    private static String[] readHeaders(BufferedReader r) throws IOException {
        r.mark(1024);
        String header = r.readLine();
        r.reset();
        return header.split("\\t");
    }

    private static void validateFile(int maxErrors, Show convert, Set<String> suppressErrors, String version,
        String arg) throws FileNotFoundException, IOException {

        int count = 0;
        try (Validator v = new Validator(new FileReader(arg, StandardCharsets.UTF_8), new BeanValidator(suppressErrors, version));) {
            v.setMaxErrors(maxErrors);
            v.setShow(convert);
            v.setName(arg);
            CVRSExtract extract = null;
            try {
                extract = v.validateOne();
            } catch (CVRSValidationException ex) {
                v.report(extract,
                    Collections.singletonList(new CVRSEntry(ex, count)),
                    count);
            }
        }
    }

    /** Headers in the tab delimited text file in order */
    private String[] headers;

    /** Validator to use for CVRSExtract beans */
    private BeanValidator validator = null;

    /** Max Errors to Allow */
    private int maxErrors = Validator.DEFAULT_MAX_ERRORS;

    /** What to Show */
    private Show convert = Show.BOTH;

    /** Name of the extract */
    private String name = "";
    private BufferedReader reader;
    private Iterable<CVRSExtract> parser = null;
    private Iterator<CVRSExtract> iterator = null;
    private int count = 0;
    private int errorCount;

    public Validator(Reader reader, BeanValidator validator) throws IOException {
        this.reader = getBufferedReader(reader);
        headers = readHeaders(this.reader);
        this.validator = validator;
        parser = ParserFactory.newParser(this.reader);
        iterator = parser.iterator();
    }

    private static BufferedReader getBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader) {
            return (BufferedReader) reader;
        } else {
            return new BufferedReader(reader);
        }
    }
    /**
     * @return the headers
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * @return The name
     */
    public String getName() {
        return name ;
    }

    /**
     * @return the suppressed
     */
    public Set<String> getSuppressed() {
        return validator == null ? Collections.emptySet() : validator.getSuppressed();
    }

    /**
     * Get the BeanValidator to use.
     * @return  the BeanValidator to use.
     */
    public BeanValidator getValidator() {
        return validator;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return validator == null ? null : validator.getVersion();
    }

    /**
     * @param headers the headers to set
     */
    public Validator setHeaders(String[] headers) {
        this.headers = headers;
        return this;
    }

    /**
     * @param name The name to set
     */
    public Validator setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the BeanValidator to use.
     * @param validator The BeanValidator.
     * @return  this
     */
    public Validator setValidator(BeanValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Set the maximum number of errors to allow in a batch.
     * @param maxErrors the maximum number of errors to allow in a batch.
     */
    public Validator setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
        return this;
    }

    // TODO: Shift this to static methods
    private Validator setShow(Show convert) {
        this.convert = convert;
        return this;
    }

    // TODO: Return Report artifact from NIST
    private int report(CVRSExtract ex, List<CVRSEntry> exList2, int count) {
        // Report captured exceptions first.
        for (CVRSEntry err: exList2) {
            System.err.printf("%s:%s\t%s%n",
                getName(),
                err.getPath() == null ? Integer.toString(err.getLine()) : err.getPath(),
                err.getDescription()
            );
            printRow(headers, err.getRow(), err.getPath(), System.err);
        }
        if (convert != Show.NEITHER) {
            ++count;
            if (convert == Show.CVRS || convert == Show.BOTH) {
                printRow(headers, ex.getValues(headers), null, System.out);
            }
            if (convert == Show.HL7 || convert == Show.BOTH) {
                try {
                    System.out.printf("%s:%d%n%s%n", getName(), count, Converter.toHL7String(ex));
                    Message m = Converter.toHL7(ex);
                    List<CVRSEntry> exList = new ArrayList<>();
                    CVRSExtract e2 = Converter.fromHL7(m, exList, validator, count);
                    Field ff = ex.notEqualsAt(e2);
                    if (ff != null) {
                        ff.setAccessible(true);
                        System.err.printf("%s:%d%n%s '%s' != '%s' %n", getName(), count,
                            "Message does not round trip at " + ff.getName(),
                            ff.get(ex), ff.get(e2)
                        );
                    }
                } catch (Exception e) {
                    System.err.printf("%s:%d%n%s%n", getName(), count, e.getMessage());
                    //e.printStackTrace();
                }
            }
        }

        return count;
    }

    public void validateHL7AsCVRS(Reader r) {

        // TODO Auto-generated method stub
        StringBuffer b = new StringBuffer();

        int msgCount = 0;
        int count = 0;
        try (BufferedReader br = r instanceof BufferedReader ? (BufferedReader)r : new BufferedReader(r);) {
            String line;
            while ((line = br.readLine()) != null) {
                count++;
                if (HL7MessageParser.isBatchDelimiter(line)) {
                    continue;
                }
                if (!line.startsWith("MSH")) {
                    System.err.println("Missing Message Header (MHS) Segment at line " + count);
                    continue;
                }

                do {
                    b.append(line).append("\r");
                    r.mark(1024);
                    count++;
                } while ((line = br.readLine()) != null && !HL7MessageParser.isMessageDelimiter(line));

                // Adjust counts.
                msgCount++;
                count--;
                r.reset();

                Message hapiMsg = p.parse(b.toString());
                List<CVRSEntry> exList = new ArrayList<>();
                CVRSExtract extract = Converter.fromHL7(hapiMsg, exList, validator, msgCount);
                count = report(extract, exList, count);
            }
        } catch (IOException e) {
            System.out.println("Error reading file at line " + count);
        } catch (HL7Exception e) {
            System.out.println("Error parsing message at line " + count + " in message " + msgCount);
        } catch (CsvException e) {
            System.out.println("Error validating message at line " + count + " in message " + msgCount);
        }
    }

    public void validateTextAsCVRS(Reader rf) {

        try (Reader r = new UpperCaseReader(rf)) {
            CsvToBean<CVRSExtract> beanReader = ParserFactory.newBeanReader(r, validator, maxErrors);

            int count = 0;
            for (CVRSExtract extract: beanReader) {
                List<CsvException> errs = beanReader.getCapturedExceptions();
                count = report(extract, toEntries(extract, errs), count);
                errs.clear();
            }
            List<CsvException> errs = beanReader.getCapturedExceptions();
            count = report(null, toEntries(null, errs), count);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<CVRSEntry> toEntries(CVRSExtract extract, List<CsvException> errs) {
        return Arrays.asList(
            errs.stream().map(
                err -> new CVRSEntry(extract, err, (int) err.getLineNumber())).toArray(CVRSEntry[]::new)
        );
    }

    public List<CVRSExtract> validate(List<CVRSValidationException> errs)
        throws IOException, CVRSValidationException
    {
        List<CVRSExtract> result = new ArrayList<CVRSExtract>();
        while (iterator.hasNext()) {
            try {
                CVRSExtract extract = validateOne();
                result.add(extract);
            } catch (CVRSValidationException e) {
                if (errs == null) {
                    throw e;
                }
                if (maxErrors != 0 && errorCount > maxErrors) {
                    throw e;
                }
                errs.add(e);
            }
        }
        return result;
    }

    public CVRSExtract validateOne() throws CVRSValidationException {
        ++count;
        CVRSExtract extract = iterator.next();
        if (validator != null) {
            try {
                validator.verifyBean(extract);
            } catch (CVRSValidationException e) {
                errorCount++;
                e.setLine(extract.getValues(headers));
                e.setLineNumber(count);
                throw e;
            }
        }
        return extract;
    }
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
