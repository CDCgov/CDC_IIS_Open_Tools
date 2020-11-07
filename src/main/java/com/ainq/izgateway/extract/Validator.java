package com.ainq.izgateway.extract;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.annotations.EventType;
import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSEntry;
import com.ainq.izgateway.extract.validation.CVRSValidationException;
import com.ainq.izgateway.extract.exceptions.CsvFieldValidationException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
/**
 * A Validator for the CDC COVID-19 Vaccination Report Specification Extract File Formats
 */
public class Validator implements Iterator<CVRSExtract>, Closeable {
    /** Bundle for Error messages */
    public static class MyResources extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return Messages;
        };
    }

    /** Default max number of errors to allow */
    public static int DEFAULT_MAX_ERRORS = 1000;

    /** Default version of CVRS to use */
    public static final String DEFAULT_VERSION = "2";
    private static final Set<String> ERROR_CODES = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            "DATA001", "DATA002", "DATA003", "DATA004", "DATA005", "DATA006", "DATA007",
            "BUSR001", "BUSR002", "BUSR003", "BUSR004", "BUSR005", "BUSR006", "BUSR007", "BUSR008", "BUSR009", "BUSR010",
            // It actually stops at 13, but we won't have to fix this later when we add more.
            "BUSR011", "BUSR012", "BUSR013", "BUSR014", "BUSR015", "BUSR016", "BUSR017", "BUSR018", "BUSR019", "BUSR020",
            // These actually stop at 3
            "REQD001", "REQD002", "REQD003", "REQD004", "REQD005",
            "DNTS001", "DNTS002", "DNTS003", "DNTS004", "DNTS005",
            // These actually stop at 1
            "HL7_001", "HL7_002", "HL7_003"
        )));
    /** Map of options to argument help text */
    private static Map<String, String> helpText = new TreeMap<>((s,t) -> s.compareToIgnoreCase(t) );

    /** Message bundle for reporting data validation errors */
    private static ResourceBundle MESSAGES = new MyResources();

    /** Localizable Error Messages */
    protected static Object[][] Messages = {
            { "DATA001", "%1$s (%2$s) contains an invalid date, should match %3$s", "Date is not correctly formatted" },
            { "DATA002", "%1$s (%3$s) should contain value %2$s", "Does not contain expected fixed value" },
            { "DATA003", "%1$s (%3$s) does not contain expected value from %2$s", "Not Used" },
            { "DATA004", "%1$s (%2$s) should not be present", "Should not be present" },
            { "DATA005", "%1$s (%3$s) does not match the regular expression %2$s", "Does not match expected format" },
            { "DATA006", "%1$s (%3$s) should contain value %2$s", "Does not contain expected value: REDACTED" },
            { "DATA007", "%1$s (%4$s) is not in value set %2$s%3$s", "Does not contain values from the expected value set" },
        };

    /**
     * Helper method to generate a validation exception with a formatted message.
     * @param extract The extract to generate the error for.
     * @param key   The Error Code (Message code) for the error
     * @param args  Arguments for message formatting.
     * @return  The exception.
     */
    public static CsvFieldValidationException error(CVRSExtract extract, String key, Object ... args) {
        return new CsvFieldValidationException(extract, key, getMessage(key, args), (String)args[0]);
    }

    /**
     * Main entry point for Command Line.
     * @param args  Command line arguments
     * @throws IOException  If a file could not be read, written or found.
     */
    public static void main(String args[]) throws IOException {
        try {
            if (args.length == 0) {
                help();
                System.exit(1);
            }
            int maxErrors = DEFAULT_MAX_ERRORS;
            Set<String> suppressErrors = new TreeSet<>();
            String version = DEFAULT_VERSION;
            boolean allOK = true;
            // Set to true to write all records regardless of validation results.
            boolean writeAll = false;
            String reportFolder = "-",
                   hl7Folder = null,
                   cvrsFolder = null;

            boolean skip = false;
            for (String arg: args) {
                if (hasArgument(arg,"-k", "Start comment")) {
                    skip = true;
                    continue;
                }
                if (hasArgument(arg,"-K", "End comment")) {
                    skip = false;
                    continue;
                }
                // If skip = true, only skip options will be processed and other arguments will be
                // skipped.  This is handy for quick debugging with script files or in Eclipse.
                // Can also be used to include Komments in parameters
                if (skip) {
                    continue;
                }

                if (hasArgument(arg,"-i", "Write all inputs to output, ignoring errors. Use with -b, -c, or -7 options")) {
                    writeAll = true;
                    continue;
                }
                if (hasArgument(arg,"-I", "Write only valid inputs to output (default)")) {
                    writeAll = false;
                    continue;
                }

                if (hasArgument(arg,"-e", "Exit the program without processing more options or arguments")) {
                    // Exit processing: Another handy tool for quick debugging in script files or Eclipse.
                    break;
                }
                if (hasArgument(arg,"-v<1|2>", "Select version (default is %s), legal values are 1 and 2", DEFAULT_VERSION)) {
                    version = arg.substring(2);
                    continue;
                }
                if (hasArgument(arg,"-m<number>", "Set the maximum number (default is %d) of errors before stopping further processing", DEFAULT_MAX_ERRORS)) {
                    // Set max number of errors before rejecting the file.
                    maxErrors = Integer.parseInt(arg.substring(2));
                    continue;
                }
                if (hasArgument(arg,"-c[folder]", "Write tab delimited version of input to <file>.txt at specified folder (default to .)")) {
                    cvrsFolder = arg.length() == 2 ? "." : arg.substring(2);
                    continue;
                }
                if (hasArgument(arg,"-n", "Do not copy inputs to any file")) {
                    cvrsFolder = null;
                    hl7Folder = null;
                    continue;
                }
                if (hasArgument(arg,"-7[folder]", "Write HL7 version of input to <file>.hl7 at specified folder (default to .)")) {
                    hl7Folder = arg.length() == 2 ? "." : arg.substring(2);
                    continue;
                }
                if (hasArgument(arg,"-b[folder]", "Write both HL7 and tab delimited version of input to <file>.hl7 and <file>.txt in specified folder (default to .)")) {
                    cvrsFolder = hl7Folder = arg.length() == 2 ? "." : arg.substring(2);
                    continue;
                }
                if (hasArgument(arg,"-s[error code,...]|ALL", "Suppress errors (e.g., -sDATA001,DATA002), use -sALL to supress all messages")) {
                    // Suppress specific errors
                    String errors = StringUtils.substring(arg, 2);
                    if ("ALL".equalsIgnoreCase(errors)) {
                        suppressErrors.clear();  // ensure later equality check works.
                        suppressErrors.addAll(ERROR_CODES);
                    } else {
                        suppressErrors.addAll(Arrays.asList(errors.split("[:,; ]+")));
                    }
                    continue;
                }
                if (hasArgument(arg,"-S[error code,...]|ALL", "Stop suppressing specified errors (e.g., -SDATA001,DATA002) or all")) {
                    if (arg.length() > 2) {
                        // Turn off specific suppressions.
                        suppressErrors.removeAll(Arrays.asList(StringUtils.substring(arg, 2).split("[:,; ]+")));
                    } else {
                        // Turn off all suppressions.
                        suppressErrors.clear();
                    }
                    continue;
                }

                if (hasArgument(arg,"-h", "Get this help")) {
                    help();
                    continue;
                }

                try (Validator v = new Validator(
                    Utility.getReader(arg),
                    suppressErrors.equals(ERROR_CODES) ? null : new BeanValidator(suppressErrors, version)
                );) {
                    v.setReport(getOutputStream(arg, reportFolder, "rpt"));
                    v.setCvrs(getOutputStream(arg, cvrsFolder, "txt"));
                    v.setHL7(getOutputStream(arg, hl7Folder, "hl7"));
                    v.setMaxErrors(maxErrors);
                    v.setName(arg);
                    v.setIgnoringErrors(writeAll);
                    v.getReport().printf("Validating %s%n", arg);

                    allOK = allOK && v.validateFile().isEmpty();
                }
            }

            System.exit(allOK ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
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

    /**
     * Given a file name and a folder and extension, create a new file name.
     * If arg already has the given extension (e.g., arg=file.txt, ext=txt), the new file
     * will have the extension .new.txt (e.g., file.new.txt) to avoid file name conflicts.
     * @param arg   The file name
     * @param folder    The folder in which to put it
     * @param ext   The new extension to use
     * @return  A new file name.
     * @throws IOException  If an error occurs creating the stream
     */
    private static PrintStream getOutputStream(String arg, String folder, String ext) throws IOException {
        if (folder == null) {
            return null;
        }

        if ("-".equals(folder)) {
            return System.out;
        }
        if ("--".equals(folder)) {
            return System.err;
        }

        File f1 = new File(arg), f2 = Utility.getNewFile(arg, new File(folder), ext);
        // Don't overwrite the input file!
        if (f1.getCanonicalPath().equals(f2.getCanonicalPath())) {
            ext = "new." + ext;
            f2 = Utility.getNewFile(arg, new File(folder), ext);
        }
        return new PrintStream(f2, StandardCharsets.UTF_8);
    }

    /**
     * Initializes help text for an option, and checks to see if arg matches the option.
     * @param arg       The argument to check.
     * @param option    The option to check for
     * @param help      The help text for the option.
     * @param args      Format arguments for the help text.
     * @return  true if the option was found.
     */
    private static boolean hasArgument(String arg, String option, String help, Object ... args) {

        if (!helpText.containsKey(option)) {
            helpText.put(option, String.format(help, args));
        }
        return arg.startsWith(option.split("[\\[\\<\\{\\(] ]")[0]);
    }

    private static void help() {
        System.out.printf("%s [options] file ... %n%n", Validator.class.getCanonicalName());
        System.out.println("Validate or convert inputs (or both) and generate a report\n\nOptions:\n");
        for (Map.Entry<String, String> entry: helpText.entrySet()) {
            System.out.printf("%s\t%s\n", entry.getKey(), entry.getValue());
        }
        System.out.println("\nfile ...\tOne or more files to validate or convert.\n");
    }

    /** All errors seen during validation */
    private List<CVRSEntry> allErrors = new ArrayList<>();

    /**
     * @return the allErrors
     */
    public List<CVRSEntry> getAllErrors() {
        return allErrors;
    }

    /** Count of messages validated */
    private int count = 0;

    /** The most recent read extract */
    private CVRSExtract currentExtract = null;

    /** The CVRS output stream */
    private PrintStream cvrs = null;

    /** Count of CVRS Records written */
    private int cvrsCount = 0;

    /** Count of messages in error */
    private int errorCount = 0;

    /** Errors associated with the most recent extract */
    private List<CVRSEntry> errors = null;

    /** Summary of Errors */
    private Map<String, Integer> errorSummary = new TreeMap<>();

    /** Headers in the tab delimited text file in order */
    private String[] headers;

    /** The HL7 output stream */
    private PrintStream hl7 = null;

    /** Count of HL7 Records written */
    private int hl7Count = 0;

    /** Set to true if errors should be ignored while writing HL7 or CVRS Output */
    private boolean ignoringErrors = false;

    /** The iterator used to loop over extracts obtained from the file */
    private Iterator<CVRSExtract> iterator = null;

    /** Max Errors to Allow */
    private int maxErrors = Validator.DEFAULT_MAX_ERRORS;

    /** Name of the extract */
    private String name = "";

    /** The Parser for converting it to a CVRSExtract */
    private Iterable<CVRSExtract> parser = null;

    /** The file being validated */
    private BufferedReader reader;

    /** The Report output stream */
    private PrintStream rpt = null;
    /** Validator to use for CVRSExtract beans */
    private BeanValidator validator = null;
    /**
     * Create a new Validator instance for the specified reader,
     * and validating using the specified BeanValidator instance.
     *
     * @param reader    The stream to read data from.
     * @param validator The BeanValidator used to valid the CVRSExtract (can be null, which skips most validation).
     * @throws IOException  If an error occured while reading content (headers)
     */
    public Validator(Reader reader, BeanValidator validator) throws IOException {
        this.reader = Utility.getBufferedReader(reader);
        headers = Utility.readHeaders(this.reader);
        if (HL7MessageParser.isMessageDelimiter(headers[0])) {
            headers = CVRSExtract.getHeaders(validator == null ? null : validator.getVersion());
        }
        this.validator = validator;
        parser = ParserFactory.newParser(this.reader, null);
        iterator = parser.iterator();
    }

    /**
     * Forces a close of the underlying reader.
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
    /**
     * @return The count of records read.
     */
    public int getCount() {
        return count;
    }
    /**
     * Get the current extract
     * @return the current extract
     */
    public CVRSExtract getCurrentExtract() {
        return currentExtract;
    }

    /**
     * Get the stream where CVRS records should be written or null
     * to skip this step.
     * @return The stream where HL7 records will be written
     */
    public PrintStream getCvrs() {
        return cvrs;
    }

    /**
     * Return the count of Cvrs records written.
     * @return the count of Cvrs records written.
     */
    public int getCvrsCount() {
        return cvrsCount;
    }

    /**
     * Get the number of records in error encounted.  A single record with one or errors will increase this count by 1, no
     * matter how many errors aree found.
     *
     * @return The count of records in error
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Get the errors associated with the current extract.
     * @return the errors
     */
    public List<CVRSEntry> getErrors() {
        return errors;
    }

    /**
     * Get the summary error report.
     * @return  A map of error codes to counts of errors found.
     */
    public Map<String, Integer> getErrorSummary() {
        return Collections.unmodifiableMap(errorSummary);
    }

    /**
     * Get the Header read from the tab delimited file.
     * @return The headers
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Get the stream where HL7 records should be written or null
     * to skip this step.
     * @return The stream where HL7 records will be written
     */
    public PrintStream getHl7() {
        return hl7;
    }

    /**
     * Return the count of HL7 records written.
     * @return the count of HL7 records written.
     */
    public int getHl7Count() {
        return hl7Count;
    }

    /**
     * Get the name of the file.
     * @return The name
     */
    public String getName() {
        return name ;
    }

    /**
     * Get the stream where the validation report should be written or null
     * to skip this step.
     * @return The stream where the validation report will be written
     */
    public PrintStream getReport() {
        return rpt;
    }

    /**
     * Get the errors being suppressed.  Only validation errors (DATA, BUSR, REQD and DNTS) can be suppressed.
     * HL7 conversion and unexpected exceptions cannot be suppressed.
     *
     * @return The codes for the suppressed errors.
     */
    public Set<String> getSuppressed() {
        return validator == null ? Collections.emptySet() : validator.getSuppressed();
    }

    /**
     * Get the BeanValidator to use.
     *
     * @return  The BeanValidator to use.
     */
    public BeanValidator getValidator() {
        return validator;
    }

    /**
     * Get the version of the CVRS to validate for.  NOTE: Source for Version 1 has had limited testing,
     * and bugs in Version 1 CVRS validation will not be fixed. However, we expect that future versions
     * of CVRS may need to be supported.  See {@link #DEFAULT_VERSION}
     *
     * @return The version of the CVRS to validate for.
     */
    public String getVersion() {
        return validator == null ? null : validator.getVersion();
    }

    /**
     * Provides the hasNext() method to allow Validator to be used with the Iterator interface
     * @return true if there are more records to process.
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Get whether this validator is ignoring errors for writing output.
     * @return true if writing invalid records.
     */
    public boolean isIgnoringErrors() {
        return ignoringErrors;
    }

    /**
     * Get the next CVRSExtract to process
     * @return the next CVRSExtract
     */
    public CVRSExtract next() {
        try {
            return validateOne();
        } catch (CVRSValidationException e) {
            NoSuchElementException ex = new NoSuchElementException();
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Set the stream where CVRS records should be written or null
     * to skip this step.
     * @param outputStream  the stream where CVRS records should be written or null
     * @return this for fluent use.
     */
    public Validator setCvrs(PrintStream outputStream) {
        cvrs = outputStream;
        if (cvrs != null) {
            Utility.printRow(cvrs, getHeaders());
        }
        return this;
    }

    /**
     * Set the stream where HL7 records should be written or null
     * to skip this step.
     * @param outputStream  the stream where HL7 records should be written or null
     * @return this for fluent use.
     */
    public Validator setHL7(PrintStream outputStream) {
        hl7 = outputStream;
        return this;
    }

    /**
     * Get whether this validator is ignoring errors for writing output.
     * @param ignoringErrors Set to true if writing should proceed for invalid records or false to write only valid records.
     * @return this for fluent use.
     */
    public Validator setIgnoringErrors(boolean ignoringErrors) {
        this.ignoringErrors = ignoringErrors;
        return this;
    }

    /**
     * Set the maximum number of errors to allow in a batch.
     * @param maxErrors the maximum number of errors to allow in a batch.
     * @return this for fluent use.
     */
    public Validator setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
        return this;
    }

    /**
     * Set the file name.
     *
     * @param name The name of the file to set.
     * @return this for fluent use.
     */
    public Validator setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the stream where the validation report should be written or null
     * to skip this step.
     * @param outputStream  the stream where CVRS records should be written or null
     * @return this for fluent use.
     */
    public Validator setReport(PrintStream outputStream) {
        rpt = outputStream;
        return this;
    }

    /**
     * Set the BeanValidator to use.
     * @param validator The BeanValidator.
     * @return  this For fluent use.
     */
    public Validator setValidator(BeanValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Collect all validated records and errors
     * @param errs  A list for reporting errors.  If null, errors will be reported in a CVRSValidationException
     * @return  The list of validate records.
     * @throws IOException  If an IO Error occured while reading.
     * @throws CVRSValidationException  If a validation error occured and errs == null, or the maximum number
     * of errors is exceeded.
     */
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

    /**
     * Validate one record, throwing an exception if an error is found.
     * @return The validated CVRSExtract
     * @throws CVRSValidationException  If an error is found.
     *
     */
    public CVRSExtract validateOne() throws CVRSValidationException {
        ++count;
        currentExtract = null;
        currentExtract = iterator.next();
        if (validator != null) {
            errors = new ArrayList<>();
            try {
                validator.verifyBean(currentExtract);
            } catch (CVRSValidationException e) {
                errorCount++;
                e.setLine(currentExtract.getValues(headers));
                e.setLineNumber(count);
                updateSummary(e);
                errors.addAll(e.getEntries());
                throw e;
            }
        }
        return currentExtract;
    }

    /**
     * Print a single detail record for validation report.
     * @param out   The stream to print to
     * @param ex    The extract being validated.
     * @param entry The validation error being reported.
     */
    private void printEntry(PrintStream out, CVRSExtract ex, CVRSEntry entry) {
        out.format("%-32s %-16s %s%n",
            StringUtils.abbreviateMiddle(getName(), "***", 32),
            StringUtils.abbreviateMiddle(ex.getVax_event_id(), "***", 16),
            entry.toString()
        );
    }

    /**
     * Report on a record.
     * @return The current record count.
     */
    private int report() {
        if (!errors.isEmpty()) {
            for (CVRSEntry err: errors) {
                printEntry(rpt, currentExtract, err);
            }
        }
        if (cvrs != null && (errors.isEmpty() || isIgnoringErrors())) {
            convertToTabDelimited();
        }
        if (hl7 != null && (errors.isEmpty() || isIgnoringErrors())) {
            convertToHL7();
        }
        return getCount();
    }

    /**
     * Write the current record to tab delimited format.
     */
    private void convertToTabDelimited() {
        cvrsCount++;
        Utility.printRow(cvrs, currentExtract.getValues(headers));
    }

    /**
     * Write the current record to HL7 format.
     */
    private void convertToHL7() {
        CVRSExtract e2 = null;
        try {
            Message m = Converter.toHL7(currentExtract);

            try {
                hl7.printf("%s%n", m.encode());
                hl7Count++;
            } catch (HL7Exception hl7ex) {
                CVRSEntry entry = new CVRSEntry(currentExtract, "HL7_003", "???",
                    hl7ex.getMessage()
                ).setLine(getCount());
                errors.add(entry);
                printEntry(rpt, currentExtract, entry);
                updateSummary(entry);
            }
            List<CVRSEntry> exList = new ArrayList<>();
            e2 = Converter.fromHL7(m, exList, validator, getCount());
            Field ff = currentExtract.notEqualsAt(e2);
            if (ff != null) {
                ff.setAccessible(true);
                CVRSEntry entry = new CVRSEntry(e2, "HL7_001", ff.getName(),
                        String.format("Message does not round trip at %s, '%s' != '%s'",
                            ff.getName(), ff.get(currentExtract), ff.get(e2)
                        )
                    ).setLine(getCount());
                errors.add(entry);
                printEntry(rpt, currentExtract, entry);
                updateSummary(entry);
            }
        } catch (Exception e) {
            CVRSEntry entry = new CVRSEntry(e2, e.getClass().getName(), "???", e.getMessage()).setLine(getCount());
            errors.add(entry);
            printEntry(rpt, currentExtract, entry);
            updateSummary(entry);
        }
    }


    /**
     * Update the summary for the given error report.
     * @param entry The reported error.
     */
    private void updateSummary(CVRSEntry entry) {
        Integer i = errorSummary.get(entry.getCategory());
        if (i == null) {
            i = Integer.valueOf(0);
        }
        errorSummary.put(entry.getCategory(), i + 1);
    }

    /**
     * Given a Validation error, update the summary with the problems found.
     * @param e The exception reported.
     */
    private void updateSummary(CVRSValidationException e) {
        for (CVRSEntry entry: e.getEntries()) {
            updateSummary(entry);
        }
    }


    /**
     * Validate a single file.
     * @return The list of errors associated with the file.
     * @throws IOException If there was an error reading the stream.
     */
    private List<CVRSEntry> validateFile() throws IOException {
        boolean reported = false;
        while (hasNext()) {
            try {
                validateOne();
                report();
            } catch (CVRSValidationException ex) {
                if (!reported) {
                    if (getReport() != null) {
                        getReport().format("%-32s %-16s %s%n", "File", "Vax_Event_Id", CVRSEntry.header());
                    }
                    reported = true;
                }
                report();
            }
            allErrors.addAll(errors);
        }

        generateSummary();
        return allErrors;
    }

    /**
     * Generate the summary output for a file
     */
    private void generateSummary() {
        Map<String, Integer> summary = getErrorSummary();
        int total = summary.values().stream().collect(Collectors.summingInt(s -> s));
        if (getReport() != null) {
            getReport().printf("%s has %d errors in %d of %d records.%n",
                getName(), total, getErrorCount(), getCount());

            if (getCvrs() != null) {
                getReport().printf("%d of %d CVRS records written.%n",
                    getCvrsCount(), getCount());

            }
            if (getHl7() != null) {
                getReport().printf("%d of %d HL7 records written.%n",
                    getHl7Count(), getCount());

            }
        }
        if (getErrorCount() != 0 && getReport() != null) {
            getReport().printf("%-8s%-8s%s%n", "Code", "Count", "Description");
            for (Map.Entry<String, Integer> e: summary.entrySet()) {
                String code = e.getKey();
                String description = getErrorDescription(code);
                getReport().printf("%-8s%5d   %s%n", code, e.getValue(), description);
            }
        }
    }

    /**
     * Get the description associated with an error code
     * @param code  The Error code
     * @return  The human readable description of the error
     */
    private String getErrorDescription(String code) {
        String description = null;
        switch(code.substring(0,4)) {
        case "DATA":
            description =
                (String) Arrays.asList(Messages).stream()
                    .filter(m -> code.equals(m[0])).findFirst().get()[2];
            break;
        case "HL7_":
            description = "Message does not round trip (possibly due to input errors).";
            break;
        case "BUSR":
            description = BeanValidator.getRule(code);
            break;
        case "REQD":
            description = "A required field is missing for " +
                EventType.values()[Integer.parseInt(code.substring(4))-1];
            break;
        case "DNTS":
            description = "A field is present that should not be for " +
                EventType.values()[Integer.parseInt(code.substring(4))-1];
            break;
        default:
            description = "Unexpected Exception: " + code;
            break;
        }
        return description;
    }
}
