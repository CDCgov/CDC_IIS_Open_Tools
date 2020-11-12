package com.ainq.izgateway.extract;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ainq.izgateway.extract.annotations.EventType;
import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSEntry;
import com.ainq.izgateway.extract.validation.CVRSValidationException;
import com.ainq.izgateway.extract.validation.NullValidator;
import com.ainq.izgateway.extract.exceptions.CsvFieldValidationException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
/**
 * A Validator for the CDC COVID-19 Vaccination Report Specification Extract File Formats
 */
public class Validator implements Iterator<CVRSExtract>, Closeable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Validator.class);
    /** Bundle for Error messages */
    public static class MyResources extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return Messages;
        };
    }

    private static class ErrorSummary {
        String code;
        String field;
        List<Triple<Integer, String, String>> examples = new ArrayList<>();

        public ErrorSummary(CVRSEntry entry, CVRSExtract extract) {
            this.code = entry.getCategory();
            this.field = entry.getPath();
            addOne(entry, extract);
        }

        void addOne(CVRSEntry entry, CVRSExtract extract) {
            examples.add(Triple.of(entry.getLine(), extract == null ? "" : extract.getVax_event_id(), entry.getDescription()));
        }
    }

    private static interface Reporter {
        /**
         * Print the header for the detail report.
         */
        void printDetailHeader();
        void printDetailRow(CVRSEntry err);
        void printDetailFooter();
        void printSummaryHeader();
        void printSummaryRow(ErrorSummary summaryRow);
        default void printSummaryFooter() {};
    }

    private Reporter consoleReporter = new Reporter() {
        boolean reported = false;
        public void printDetailHeader() {
            reported = false;
            getReport().printf("Validating %s%n", getName());
        }

        /**
         * Print a single detail record for validation report.
         * @param out   The stream to print to
         * @param ex    The extract being validated.
         * @param entry The validation error being reported.
         */
        @Override
        public void printDetailRow(CVRSEntry entry) {
            if (!reported) {
                getReport().format("%-32s %-16s %s%n", "File", "Vax_Event_Id", CVRSEntry.header());
                reported = true;
            }
            getReport().format("%-32s %-16s %s%n",
                StringUtils.abbreviateMiddle(getName(), "***", 32),
                StringUtils.abbreviateMiddle(currentExtract.getVax_event_id(), "***", 16),
                entry.toString()
            );
        }

        @Override
        public void printDetailFooter() {
            Map<String, ErrorSummary> summary = getErrorSummary();
            int total = summary.values().stream()
                            .collect(Collectors.summingInt(s -> s == null ? 0 : s.examples.size()));
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

        @Override
        public void printSummaryHeader() {
            if (getErrorCount() > 0) {
                getReport().printf("%-8s%-24s%-8s%-52s%s%n", "Code", "Field", "Count", "Description", "Example");
            }
        }

        @Override
        public void printSummaryRow(ErrorSummary summaryRow) {
            String description = getErrorDescription(summaryRow.code);
            getReport().printf("%-8s%-24s%5d   %-52s %5d %s%n",
                summaryRow.code, summaryRow.field,
                summaryRow.examples.size(), description, summaryRow.examples.get(0).getLeft(), summaryRow.examples.get(0).getRight());
        }
    };

    private Reporter jsonReporter = new Reporter() {
        JsonObjectBuilder b = Json.createObjectBuilder();
        JsonArrayBuilder a = null;
        public void printDetailHeader() {
            b.add("filename", getName());
            a = Json.createArrayBuilder();
        }

        /**
         * Print a single detail record for validation report.
         * @param out   The stream to print to
         * @param ex    The extract being validated.
         * @param entry The validation error being reported.
         */
        @Override
        public void printDetailRow(CVRSEntry entry) {
            if (a == null) {
                throw new IllegalStateException("printDetailHeader must be called first");
            }

            JsonObjectBuilder obj = Json.createObjectBuilder();
            obj.add("vax_event_id",  currentExtract == null ? "" : currentExtract.getVax_event_id());
            obj.add("line", entry.getLine());
            obj.add("code", entry.getCategory());
            obj.add("field", entry.getPath());
            obj.add("level", entry.getClassification());
            obj.add("message",  entry.getDescription());
            a.add(obj.build());
        }

        @Override
        public void printDetailFooter() {
            b.add("detail", a.build());
            Map<String, ErrorSummary> summary = getErrorSummary();
            int total = summary.values().stream()
                            .collect(Collectors.summingInt(s -> s == null ? 0 : s.examples.size()));
            if (getReport() != null) {
                b.add("totalErrors", total);
                b.add("failedRecords",  getErrorCount());
                b.add("totalRecords", getCount());
                if (getCvrs() != null) {
                    b.add("cvrsWritten",  getCvrsCount());
                }
                if (getHl7() != null) {
                    b.add("hl7Written",  getHl7Count());
                }
            }
        }

        @Override
        public void printSummaryHeader() {
            a = Json.createArrayBuilder();
        }

        @Override
        public void printSummaryRow(ErrorSummary summaryRow) {
            if (a == null) {
                throw new IllegalStateException("printSummaryHeader must be called first");
            }

            JsonObjectBuilder obj = Json.createObjectBuilder();
            obj.add("code", summaryRow.code);
            obj.add("field", summaryRow.field);
            obj.add("count", summaryRow.examples.size());
            obj.add("description", getErrorDescription(summaryRow.code));
            JsonArrayBuilder a2 = Json.createArrayBuilder();
            for (Triple<Integer, String, String> t: summaryRow.examples) {
                JsonObjectBuilder obj2 = Json.createObjectBuilder();
                obj2.add("line", t.getLeft());
                obj2.add("vax_event_id", t.getMiddle());
                obj2.add("message", t.getRight());
                a2.add(obj2.build());
            }
            obj.add("examples", a2.build());
            a.add(obj.build());
        }

        public void printSummaryFooter() {
            b.add("summary", a.build());
            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);

            JsonWriterFactory jwf = Json.createWriterFactory(config);
            JsonWriter jsonWriter = jwf.createWriter(rpt);
            jsonWriter.writeObject(b.build());
        }

    };

    /** Localizable Error Messages */
    protected static Object[][] Messages = {
            { "DATA001", "%1$s (%2$s) contains an invalid date, should match %3$s", "Date is not valid" },
            { "DATA002", "%1$s (%3$s) should contain value %2$s", "Does not contain expected fixed value" },
            { "DATA003", "%1$s (%3$s) is not a legal value for extract type %2$s", "Extract Type Invalid" },
            { "DATA004", "%1$s (%2$s) should not be present", "Should not be present" },
            { "DATA005", "%1$s (%3$s) does not match the regular expression %2$s", "Does not match expected format" },
            { "DATA006", "%1$s (%3$s) should contain value %2$s", "Does not contain expected value: REDACTED" },
            { "DATA007", "%1$s (%4$s) is not in value set %2$s%3$s", "Does not contain values from the expected value set" },
            { "DATA008", "%1$s (%2$s) exceeds maximum length %3$s", "Field exceeds maximum length" },
            { "DATA009", "%1$s (%2$s) contains an incorrectly formatted date, should match %3$s", "Date is not correctly formatted" },
        };

    /** Default version of CVRS to use */
    public static final String DEFAULT_VERSION = "2";
    private static final Set<String> ERROR_CODES = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            "DATA001", "DATA002", "DATA003", "DATA004", "DATA005", "DATA006", "DATA007", "DATA008", "DATA009",
            "BUSR001", "BUSR002", "BUSR003", "BUSR004", "BUSR005", "BUSR006", "BUSR007", "BUSR008", "BUSR009", "BUSR010",
            // It actually stops at 13, but we won't have to fix this later when we add more.
            "BUSR011", "BUSR012", "BUSR013", "BUSR014", "BUSR015", "BUSR016", "BUSR017", "BUSR018", "BUSR019", "BUSR020",
            // These actually stop at 3
            "REQD001", "REQD002", "REQD003", "REQD004", "REQD005",
            "DNTS001", "DNTS002", "DNTS003", "DNTS004", "DNTS005",
            // These actually stop at 1
            "HL7_001", "HL7_002", "HL7_003"
        )));

    private static final String INVALID_DATE_FORMAT = "DATA009";
    /** Default max number of errors to allow */
    public static int DEFAULT_MAX_ERRORS = 1000;

    /** Message bundle for reporting data validation errors */
    private static ResourceBundle MESSAGES = new MyResources();

    /** Map of options to argument help text */
    private static Map<String, String> helpText = new TreeMap<>((s,t) -> s.compareToIgnoreCase(t) );

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
     * Gyration to support testing
     * @param args  Command line arguements
     * @throws IOException If a file could not be read, written or found.
     */
    public static void main(String... args) throws IOException {
        System.exit(main1(args, "-"));
    }

    /**
     * Main entry point for Command Line.
     * @param args  Command line arguments
     * @param reportFolder The folder where reports should be placed.
     * If reportFolder is set to "-", reports will be written System.out.  If set to "--" reports will be written to System.err;
     * @return The number of validation errors found, or 0 if none. Values less than 0 indicate an execution error (e.g., missing input file).
     * @throws IOException  If a file could not be read, written or found.
     */
    public static int main1(String args[], String reportFolder) throws IOException {
        try {
            if (args.length == 0) {
                help();
                return 1;
            }
            int maxErrors = DEFAULT_MAX_ERRORS;
            int totalErrors = 0;
            Set<String> suppressErrors = new TreeSet<>();
            String version = DEFAULT_VERSION;
            // Set to true to write all records regardless of validation results.
            boolean writeAll = false, useDefaults = false;
            String hl7Folder = null,
                   cvrsFolder = null;
            boolean useJson = false;
            boolean skip = false;
            boolean fixIt = false;
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

                if (hasArgument(arg,"-f", "Attempt to correct the value")) {
                    writeAll = true;
                    continue;
                }
                if (hasArgument(arg,"-F", "Stop correcting values")) {
                    writeAll = false;
                    continue;
                }

                if (hasArgument(arg, "-j", "Write output report in JSON format")) {
                    useJson = true;
                    continue;
                }

                if (hasArgument(arg, "-J", "Write output report in text format")) {
                    useJson = false;
                    continue;
                }

                if (hasArgument(arg,"-e", "Exit the program without processing more options or arguments")) {
                    // Exit processing: Another handy tool for quick debugging in script files or Eclipse.
                    break;
                }
                if (hasArgument(arg,"-d", "Enable default conversion rules for HL7 Messages")) {
                    useDefaults = true;
                    continue;
                }
                if (hasArgument(arg,"-D", "Disable default conversion rules for HL7 Messages")) {
                    useDefaults = false;
                    continue;
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

                if (hasArgument(arg,"-r[folder]", "Write the repoort to <file>.rpt at the specified folder (defaults to standard output)") ) {
                    reportFolder = arg.length() == 2 ? "-" : arg.substring(2);
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

                String files[] = { arg };
                if (arg.contains("*") || arg.contains("?")) {
                    File f = new File(arg);
                    Collection<File> list = FileUtils.listFiles(f.getParentFile(),  new WildcardFileFilter(f.getName()), null);
                    files = new String[list.size()];
                    int i = 0;
                    for (File found: list) {
                        files[i++] = found.getPath();
                    }
                }

                totalErrors += validateFiles(reportFolder, maxErrors, suppressErrors, version, writeAll, useJson,
                    useDefaults, fixIt, hl7Folder, cvrsFolder, files);
            }

            return totalErrors;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Validate one ore more files
     * @param reportFolder  Where to send the report
     * @param maxErrors     Maximum number of errors
     * @param suppressErrors    List of errors to suppress
     * @param version   Version of CVRS to validate against
     * @param writeAll  Whether or not to write all or only valid extracts during conversion
     * @param useJson   Whether or not to use JSON for Reporting
     * @param useDefaults   Whether or not to use HL7 Message Defaults for missing segments
     * @param hl7Folder Where to put converted HL7 Messages
     * @param cvrsFolder Where to put converted CVRS Tab Delimited Output
     * @param files The files to process
     * @return  The number of errors found
     * @throws IOException  If a read error occurs
     */
    public static int validateFiles(
        String reportFolder,
        int maxErrors,
        Set<String> suppressErrors,
        String version,
        boolean writeAll,
        boolean useJson,
        boolean useDefaults,
        boolean fixIt,
        String hl7Folder,
        String cvrsFolder,
        String[] files
    ) throws IOException {
        int errors = 0;
        for (String file: files) {
            try (Validator v = new Validator(
                Utility.getReader(file),
                suppressErrors.equals(ERROR_CODES) ?
                    new NullValidator(suppressErrors, version) :
                    new BeanValidator(suppressErrors, version, fixIt),
                useDefaults
            );) {
                v.setReport(getOutputStream(file, reportFolder, useJson ? "rpt.json" : "rpt"));
                v.setCvrs(getOutputStream(file, cvrsFolder, "txt"));
                v.setHL7(getOutputStream(file, hl7Folder, "hl7"));
                v.setMaxErrors(maxErrors);
                v.setName(file);
                v.setFixIt(fixIt);
                v.setIgnoringErrors(writeAll);
                if (useJson) {
                    v.reporter = v.jsonReporter;
                }
                if (!v.getReport().equals(System.out)) {
                    System.out.printf("Validating %s%n", file);
                }
                errors += v.validateFile().size();
            } catch (IOException ioex) {
                System.err.printf("Error processing %s: %s%n", file, ioex.getMessage());
                ioex.printStackTrace();
                errors += 1;
            }
        }
        return errors;
    }

    /** Helper method to get a formatted message
     *
     * @param key   Message Code (Error Code)
     * @param args  Arguments to generate the message
     * @return The formatted message.
     */
    public static String getMessage(String key, Object ... args) {
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
        File dir = f2.getCanonicalFile().getParentFile();

        if (!dir.exists()) {
            dir.mkdirs();
        }
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
        String opt = option.split("[\\Q[<{(] \\E]")[0];
        return arg.startsWith(opt);
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

    /** Errors associated with the most recent extract */
    private List<CVRSEntry> errors = new ArrayList<>();

    /** Summary of Errors */
    private Map<String, ErrorSummary> errorSummary = new TreeMap<>();

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

    /** If true, use default conversion rules for missing fields */
    private boolean useDefaults;

    /** If true, reformat invalid but recognizable dates */
    private boolean reformatDates = false;

    /** The reporter to use, defaults to console (text) reporter */
    private Reporter reporter = consoleReporter;

    /** The number of records in error */
    private int errorCount = 0;

    /** Set to true to fix up values to something legitimately close */
    private boolean fixIt = false;
    /**
     * Create a new Validator instance for the specified reader,
     * and validating using the specified BeanValidator instance.
     *
     * @param reader    The stream to read data from.
     * @param validator The BeanValidator used to valid the CVRSExtract (can be null, which skips most validation).
     * @param useDefaults If true, use default values for missing fields in HL7 messages.
     * @throws IOException  If an error occured while reading content (headers)
     */
    public Validator(Reader reader, BeanValidator validator, boolean useDefaults) throws IOException {
        this.reader = Utility.getBufferedReader(reader);
        String[] validHeaders = CVRSExtract.getHeaders(validator == null ? DEFAULT_VERSION : validator.getVersion());
        headers = Utility.readHeaders(this.reader);
        errors = new ArrayList<>();

        if (headers != null) {
            // Check for a Byte Order Mark
            if (headers[0].charAt(0) == '\ufeff') {
                CVRSEntry e = new CVRSEntry(null, "FMT_005", "Header", "File starts with a Unicode Byte Order Mark (FEFF)");
                addError(e);
                // Strip the BOM and continue
                headers[0] = headers[0].substring(1);
                this.reader.skip(1);
            }
            if (HL7MessageParser.isMessageDelimiter(headers[0])) {
                headers = validHeaders;
            } else if (headers.length == 1) {
                CVRSEntry e = new CVRSEntry(null, "FMT_001", "Header", "File is not tab delimited. First Row: " + headers[0]);
                addError(e);
                headers = headers[0].replace("\"","").split("\\s*,\\s*");
                reformatDates = true;  // Presume that dates need to be reformed.
            }
        }
        checkHeaders(validHeaders);
        this.validator = validator;
        parser = ParserFactory.newParser(this.reader, null, useDefaults);
        iterator = parser.iterator();
    }

    private void addError(CVRSEntry e) {
        if (errors.size() == 0) {
            errorCount++;
        }
        errors.add(e);
        updateSummary(e);
        // We cannot report errors during construction
        if (count > 0) {
            reporter.printDetailRow(e);
        }
    }

    /**
     *  Verify the headers match the valid headers.
     *  @param validHeaders The valid headers to check against.
     */
    private void checkHeaders(String[] validHeaders) {
        // Normalize headers before check
        CVRSEntry e = null;
        if (headers == null) {
            e = new CVRSEntry(null, "FMT_003", "Header", "File is not a CVRS");
            addError(e);
            return;
        }
        for (int i = 0; i < headers.length;  i++) {
            headers[i] = headers[i].toLowerCase();
        }
        List<String> goodHeaders = new ArrayList<>(Arrays.asList(validHeaders));
        if (headers != validHeaders && !goodHeaders.containsAll(Arrays.asList(headers))) {
            // Collect the bad headers.
            List<String> badHeaders =
                Arrays.asList(headers).stream()
                    .filter(h -> !goodHeaders.contains(h)).collect(Collectors.toList());
            List<String> actualHeaders = new ArrayList<>();
            actualHeaders.addAll(Arrays.asList(headers));
            e = new CVRSEntry(null, "FMT_002", "Header", String.format("Input contains invalid headers: %s", badHeaders));
            addError(e);
            actualHeaders.removeAll(badHeaders);
            headers = actualHeaders.toArray(new String[actualHeaders.size()]);
            if (actualHeaders.size() == 0) {
                e = new CVRSEntry(null, "FMT_003", "Header", "File is not a CVRS");
                addError(e);
                return;
            }
        }

        if (headers.length < validHeaders.length) {
            goodHeaders.removeAll(Arrays.asList(headers));
            e = new CVRSEntry(null, "FMT_004", "Header", "File is missing headers " + goodHeaders);
            addError(e);
        }
    }

    /**
     * Forces a close of the underlying reader, and all ouput files
     * that are not stderr or stdout.
     */
    @Override
    public void close() throws IOException {
        Arrays.asList(getReport(), this.getHl7(), this.getCvrs()).stream().forEach( s -> {
            if (s != null && !s.equals(System.out) && !s.equals(System.err)) {
                s.close();
            }
        });
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
    public Map<String, ErrorSummary> getErrorSummary() {
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
     * @return the fixIt
     */
    public boolean isFixIt() {
        return fixIt;
    }

    /**
     * @return the reformatDates
     */
    public boolean isReformatDates() {
        return reformatDates;
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

    public boolean getUseDefaults() {
        return useDefaults;
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
     * @param fixIt the fixIt to set
     * @return this for fluent use.
     */
    public Validator setFixIt(boolean fixIt) {
        this.fixIt = fixIt;
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
     * @param reformatDates the reformatDates to set
     */
    public void setReformatDates(boolean reformatDates) {
        this.reformatDates = reformatDates;
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

    public Validator setUseDefaults(boolean useDefaults) {
        this.useDefaults = useDefaults;
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
                if (maxErrors != 0 && getErrorCount() > maxErrors) {
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
        if (count > 1) {
            // Don't clear header errors.
            errors.clear();
        } else if (count == 1) {
            // If this is the very first record,
            // we may have some errors to report
            // even before validating.
            for (CVRSEntry e: errors) {
                reporter.printDetailRow(e);
            }
        }
        if (validator != null) {
            try {
                validator.verifyBean(currentExtract);
            } catch (CVRSValidationException ex) {
                ex.setLine(currentExtract.getValues(headers));
                ex.setLineNumber(count);
                ex.getEntries().forEach(e -> addError(e));
                if (reformatDates ) {
                    ex.getEntries().stream()
                      .filter(e -> Validator.INVALID_DATE_FORMAT.equals(e.getCategory()))
                      .forEach(e -> reformatDate(e) );
                }
                throw ex;
            }
        }
        return currentExtract;
    }

    private void reformatDate(CVRSEntry e) {
        // Get the broken value
        String value = currentExtract.getField(e.getPath());
        value = value.replace("^([0-9]+)/([0-9]+)(/([0-9]+))?$", "$3-$1-$2");
        currentExtract.setField(e.getPath(), value);
    }

    /**
     * Convert data
     * @return The current record count.
     */
    private int convert() {
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
            Message m = Converter.toHL7(currentExtract, validator != null ? validator.getVersion() : DEFAULT_VERSION);

            try {
                hl7.printf("%s%n", m.encode());
                hl7Count++;
            } catch (HL7Exception hl7ex) {
                CVRSEntry entry = new CVRSEntry(currentExtract, "HL7_003", "???",
                    hl7ex.getMessage()
                ).setLine(getCount());
                addError(entry);
            }
            List<CVRSEntry> exList = new ArrayList<>();
            e2 = Converter.fromHL7(m, exList, validator, useDefaults, getCount());
            Field ff = currentExtract.notEqualsAt(e2);
            if (ff != null) {
                ff.setAccessible(true);
                CVRSEntry entry = new CVRSEntry(e2, "HL7_001", ff.getName(),
                        String.format("Message does not round trip at %s, '%s' != '%s'",
                            ff.getName(), ff.get(currentExtract), ff.get(e2)
                        )
                    ).setLine(getCount());
                addError(entry);
            }
        } catch (Exception e) {
            CVRSEntry entry = new CVRSEntry(e2, e.getClass().getName(), "???", e.getMessage()).setLine(getCount());
            addError(entry);
        }
    }


    /**
     * Update the summary for the given error report.
     * @param entry The reported error.
     */
    private void updateSummary(CVRSEntry entry) {
        String key = String.format("%-8s%s" , entry.getCategory(), entry.getPath());
        ErrorSummary summaryRow = errorSummary.get(key);
        if (summaryRow == null) {
            errorSummary.put(key, summaryRow = new ErrorSummary(entry, currentExtract));
        } else {
            summaryRow.addOne(entry, currentExtract);
        }
    }

    /**
     * Validate a single file.
     * @return The list of errors associated with the file.
     * @throws IOException If there was an error reading the stream.
     */
    private List<CVRSEntry> validateFile() throws IOException {
        if (getReport() != null) {
            reporter.printDetailHeader();
        }
        while (hasNext()) {
            try {
                validateOne();
                convert();
            } catch (CVRSValidationException ex) {
                convert();
            }
            allErrors.addAll(errors);
        }
        if (count == 0) {
            convert();
        }
        if (getReport() != null) {
            generateSummary();
        }
        return allErrors;
    }

    /**
     * Generate the summary output for a file
     */
    private void generateSummary() {

        reporter.printDetailFooter();
        reporter.printSummaryHeader();
        for (ErrorSummary summaryRow: getErrorSummary().values()) {
            reporter.printSummaryRow(summaryRow);
        }
        reporter.printSummaryFooter();
    }

    /**
     * Get the description associated with an error code
     * @param code  The Error code
     * @return  The human readable description of the error
     */
    private static String getErrorDescription(String code) {
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
        case "FMT_":
            description = "There is an error in the file format.";
            break;
        default:
            description = "Unexpected Exception: " + code;
            break;
        }
        return description;
    }
}
