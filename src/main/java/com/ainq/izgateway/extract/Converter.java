package com.ainq.izgateway.extract;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.ainq.izgateway.extract.annotations.FieldValidator;
import com.ainq.izgateway.extract.annotations.V2Field;
import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSEntry;
import com.ainq.izgateway.extract.validation.DateValidator;
import com.ainq.izgateway.extract.validation.DateValidatorIfKnown;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvFieldValidationException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v251.datatype.CE;
import ca.uhn.hl7v2.model.v251.message.VXU_V04;
import ca.uhn.hl7v2.model.v251.segment.OBX;
import ca.uhn.hl7v2.util.Terser;
public class Converter {
    public final static String[] SEGMENT_ORDER = { "MSH", "PID", "ORC", "RXA", "RXR", "OBX" };

    public static CVRSExtract fromHL7(Message message, List<CVRSEntry> exList, BeanValidator validator, int line) throws CsvException {
        Terser terser = new Terser(message);
        CVRSExtract extract = new CVRSExtract();

        extract.forEachField(true, field -> {
            field.setAccessible(true);
            V2Field v2Data = field.getAnnotation(V2Field.class);
            FieldValidator val = field.getAnnotation(FieldValidator.class);
            CsvBindByName binder = field.getAnnotation(CsvBindByName.class);
            if (v2Data == null) {
                throw new RuntimeException(field.getName() + " is not annotated.");
            }
            String value = null;

            try {
                if (v2Data.obx3() != null && v2Data.obx3().length() != 0) {
                    // Special handling to find the right OBX
                    List<Segment> obxList = getAllOrderObxSegments(message);
                    value = "";
                    for (Segment obx: obxList) {
                        CE id;
                        id = (CE) obx.getField(3, 0);
                        String code = id.getIdentifier().getValue();
                        String system = id.getNameOfCodingSystem().getValue();
                        String obx3Parts[] = v2Data.obx3().split("\\^");
                        if (obx3Parts.length > 0 && obx3Parts[0].equalsIgnoreCase(code)) {
                            if (obx3Parts.length <= 2 || obx3Parts[2].equalsIgnoreCase(system)) {
                                value = adjustValuesToExtract(obx.getField(5, 0), val, v2Data.map());
                                break;
                            }
                        }
                    }
                } else {
                    value = adjustValuesToExtract(terser.get("/." + v2Data.value()), val, v2Data.map());
                }
            } catch (HL7Exception e) {
                throw new RuntimeException("Unexpected HL7Exception", e);
            }
            field.set(extract, value);
            if (validator != null && binder.required()) {
                CsvFieldValidationException ex =
                    new CsvFieldValidationException(extract, "REQD002", field.getName(), v2Data.value() + " does not contain required " + field.getName() + " value.")
                        .setErrorPosition(v2Data.value());
                if (exList == null) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
                exList.add(ex.getValidationEntry());
            }
        });


        if (exList != null) {
            // Add data to line values in Error entries.
            String values[] = extract.getValues();
            for (CVRSEntry ex: exList) {
                ex.setRow(values);
                ex.setLine(line);
            }
        }
        return extract;
    }

    public static void main(String args[]) {
        boolean skip = false;
        Converter converter = new Converter();
        List<File> inputs = new ArrayList<>();
        File outputFolder = new File(".");
        boolean disableValidation = false;

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

            if (arg.startsWith("-d")) {
                // Disable input validation (used to support conversion
                // between formats for test files having errors in them).
                disableValidation = true;
                continue;
            }

            if (arg.startsWith("-e")) {
                // Exit processing: Another handy tool for quick debugging in script files or Eclipse.
                break;
            }

            if (arg.startsWith("-i")) {
                String input = arg.substring(2);
                File f = new File(input);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        inputs.addAll(FileUtils.listFiles(f, TrueFileFilter.INSTANCE, null));
                    } else {
                        inputs.add(f);
                    }
                } else if (f.getName().contains("*") || f.getName().contains("?")) {
                    inputs.addAll(FileUtils.listFiles(f.getParentFile(), new WildcardFileFilter(f.getName()), null));
                }
                continue;
            }

            if (arg.startsWith("-o")) {
                String output = arg.substring(2);
                File f = new File(output);
                if (f.exists() && f.isDirectory()) {
                    outputFolder = f;
                } else if (!f.exists()) {
                    f.mkdirs();
                    outputFolder = f;
                }
                continue;
            }

            if (arg.startsWith("-v")) {
                String version = arg.substring(2);
                converter.setVersion(version);
                continue;
            }

            if (arg.startsWith("-m")) {
                // Set max number of errors before rejecting the file.
                int maxErrors = Integer.parseInt(arg.substring(2));
                converter.setMaxErrors(maxErrors);
                continue;
            }

            if (arg.startsWith("-s")) {
                // Suppress specific errors
                converter.setSuppressed(Arrays.asList(StringUtils.substring(arg, 2).split("[:,; ]+")));
                continue;
            }
            if (arg.startsWith("-S")) {
                if (arg.length() > 2) {
                    // Turn off specific suppressions.
                    converter.setSuppressed(Arrays.asList(StringUtils.substring(arg, 2).split("[:,; ]+")));
                } else {
                    // Turn off all suppressions.
                    converter.setSuppressed(Collections.emptySet());
                }
                continue;
            }

            File f = new File(arg);
            if (!f.exists()) {
                System.err.println("File does not exist: " + arg);
                continue;
            }


        }
        if (disableValidation) {
            converter.setValidationEnabled(false);
        }
        doConvert(inputs, outputFolder, converter);
    }

    private static void doConvert(List<File> inputs, File outputFolder, Converter converter) {
        if (inputs.isEmpty()) {
            System.err.println("Nothing to convert.");
            return;
        }
        boolean toHL7 = getDirection(inputs);

        for (File f: inputs) {
            try (FileReader r = new FileReader(f, StandardCharsets.UTF_8);
                FileWriter w = new FileWriter(getNewFile(f, outputFolder, toHL7), StandardCharsets.UTF_8)) {
                Iterable<CVRSExtract> parser = ParserFactory.newParser(r);
                if (!toHL7) {
                    writeRow(w, CVRSExtract.getHeaders());
                }
                for (CVRSExtract extract: parser) {
                    if (toHL7) {
                        w.write(toHL7String(extract));
                    } else {
                        writeRow(w, extract.getValues());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void writeRow(FileWriter w, String[] values) throws IOException {
        w.write(StringUtils.defaultString(values[0]));
        for (int i = 1; i < values.length; i++) {
            w.write("\t");
            w.write(StringUtils.defaultString(values[i]).replace("\t", " "));
        }
    }

    private static File getNewFile(File f, File outputFolder, boolean toHL7) {
        String name = f.getName();
        String file = StringUtils.substringBeforeLast(name, ".");
        String ext = toHL7 ? "hl7" : "txt";
        return new File(outputFolder, String.format("%s.%s", file, ext));
    }

    private static boolean getDirection(List<File> inputs) {
        boolean toHL7 = false;
        while (!inputs.isEmpty()) {
            File first = inputs.get(0);
            try {
                String s = FileUtils.readFileToString(first, StandardCharsets.UTF_8);
                toHL7 = !s.startsWith("MSH");
                break;
            } catch (IOException e) {
                System.err.println("File does not exist: " + first.getAbsolutePath());
                inputs.remove(0);
            }
        }
        return toHL7;
    }

    public static VXU_V04 toHL7(CVRSExtract e) {
        VXU_V04 message = new VXU_V04();
        try {
            message.initQuickstart("VXU", "V04", "P");
        } catch (HL7Exception | IOException e2) {
            throw new RuntimeException("Cannot initialize VXU_V04", e2);
        }
        Field[] fields = e.getClass().getDeclaredFields();
        Arrays.sort(fields, Converter::compareFields);
        Terser terser = new Terser(message);

        e.forEachField(true, field -> {
            try {
                V2Field v2Data = field.getAnnotation(V2Field.class);
                FieldValidator val = field.getAnnotation(FieldValidator.class);
                if (v2Data == null) {
                    throw new RuntimeException(field.getName() + " is not annotated.");
                }
                field.setAccessible(true);
                String value = adjustValuesToHL7(field.get(e), val, v2Data.map());

                if (value != null && value.trim().length() != 0) {
                    boolean isObx = false;
                    if (v2Data.obx3() != null && v2Data.obx3().length() != 0) {
                        setObx3(message, v2Data);
                        isObx = true;
                    }
                    setValue(terser, v2Data, value, isObx);
                }
            } catch (HL7Exception e1) {
                throw new RuntimeException("Unexpected HL7Exception", e1);
            }
        });

        return message;
    }

    public static String toHL7String(CVRSExtract e)  {
        VXU_V04 message = toHL7(e);
        try {
            return message.encode();
        } catch (HL7Exception e1) {
            throw new RuntimeException("Exception encoding to VXU", e1);
        }
    }

    private static String adjustValuesToExtract(String value, FieldValidator val, String[] map) {
        if (value == null) {
            value = "";
        }
        if (val != null) {
            if (val.validator() == DateValidator.class || val.validator() == DateValidatorIfKnown.class) {
                StringBuffer b = new StringBuffer();
                if (value.length() > 0) {
                    b.append(StringUtils.substring(value, 0, 4));
                    if (value.length() > 4) {
                        b.append("-").append(StringUtils.substring(value, 4, 6));
                        if (value.length() > 6) {
                            b.append("-").append(StringUtils.substring(value, 6, 8));
                        }
                    }
                }
                return b.toString();
            }
        }
        if (map == null || map.length == 0) {
            return value;
        }
        for (int i = 0; i < map.length; i += 2) {
            if (i == map.length - 1) {
                // final value in odd length maps is default to set if no mapping found.
                // we don't know HOW to unmap it.
                return null;
            }
            if (value.equalsIgnoreCase(StringUtils.substringBefore(map[i+1],"^")) || map[i+1].length() == 0) {
                return map[i];
            }
        }
        return "";
    }

    private static String adjustValuesToExtract(Type field, FieldValidator val, String[] map) {
        if (field instanceof Primitive) {
            return adjustValuesToExtract(((Primitive) field).getValue(), val, map);
        } else if (field instanceof Varies) {
            return adjustValuesToExtract(((Varies) field).getData(), val, map);
        }
        try {
            Composite comp = (Composite) field;
            if (comp.getComponents().length == 0) {
                return adjustValuesToExtract("", val, map);
            }
            return adjustValuesToExtract(comp.getComponent(0), val, map);
        } catch (DataTypeException e) {
            // Won't happen.
            return null;
        }
    }


    private static String adjustValuesToHL7(Object object, FieldValidator val, String[] map) {
        String value = (String) object;
        if (val != null) {
            if (val.validator() == DateValidator.class || val.validator() == DateValidatorIfKnown.class) {
                return value.replace("-", "");
            }
        }
        if (map == null || map.length == 0) {
            return value;
        }
        for (int i = 0; i < map.length; i += 2) {
            if (i == map.length - 1) {
                // final value in odd length maps is default to set if no mapping found.
                return map[i];
            }
            if (value.equalsIgnoreCase(map[i])) {
                return map[i + 1].length() == 0 ? null : map[i+1];
            }
        }
        return null;
    }

    public static int compareFields(Field o1, Field o2) {
        V2Field a1 = o1.getAnnotation(V2Field.class), a2 = o2.getAnnotation(V2Field.class);
        if (a1 == a2) { return 0; }
        if (a1 == null && a2 != null) { return 1; }
        if (a1 != null && a2 == null) { return -1; }

        if (a1.value().equals(a2.value())) {
            return 0;
        }

        String seg1 = a1.value().substring(0, 3), seg2 = a2.value().substring(0, 3);
        int comp = segmentIndex(seg1) - segmentIndex(seg2);
        if (comp  != 0) {
            return comp;
        }

        String p1[] = a1.value().split("[-\\(\\)\\.]"),
               p2[] = a2.value().split("[-\\(\\)\\.]");

        for (int i = 1; i < Math.min(p1.length, p2.length); i++) {
            comp = Integer.parseInt(p1[i]) - Integer.parseInt(p2[i]);
            if (comp != 0) {
                return comp;
            }
        }
        return p1.length - p2.length;
    }

    private static List<Segment> getAllOrderObxSegments(Message message) {
        List<Segment> segments = new ArrayList<>();
        Structure[] orders;
        try {
            orders = message.getAll("ORDER");
            for (Structure order: orders) {
                Structure obses[] = ((Group)order).getAll("OBSERVATION");
                for (Structure obs: obses) {
                    Structure[] obx = ((Group)obs).getAll("OBX");
                    if (obx != null && obx.length > 0) {
                        segments.add((Segment) obx[0]);
                    }
                }
            }
            return segments;
        } catch (HL7Exception e) {
            throw new RuntimeException("HL7 Parser Error", e);
        }
    }


    private static int segmentIndex(String seg) {
        for (int i = 0; i < SEGMENT_ORDER.length; i++) {
            if (SEGMENT_ORDER[i].equals(seg)) {
                return i;
            }
        }
        return SEGMENT_ORDER.length;
    }

    private static void setObx3(VXU_V04 message, V2Field v2Data) throws HL7Exception, DataTypeException {
        OBX obx = message.getORDER().insertOBSERVATION(0).getOBX();
        obx.getValueType().setValue("CE");
        String obx3[] = v2Data.obx3().split("\\^");
        if (obx3.length > 0) {
            obx.getObservationIdentifier().getIdentifier().setValue(obx3[0]);
        }
        if (obx3.length > 1) {
            obx.getObservationIdentifier().getText().setValue(obx3[1]);
        }
        if (obx3.length > 2) {
            obx.getObservationIdentifier().getNameOfCodingSystem().setValue(obx3[2]);
        }
    }

    private static void setValue(Terser terser, V2Field v2Data, String value, boolean isObx) throws HL7Exception {
        if (isObx && value.contains("^")) {
            String parts[] = value.split("\\^");
            for (int i = 0; i < parts.length; i++) {
                terser.set("/." + v2Data.value() + "-" + (i + 1), parts[i]);
            }
        } else {
            terser.set("/." + v2Data.value(), value);
        }
        if (v2Data.system().length() != 0) {
            String s = StringUtils.substringBeforeLast(v2Data.value(), "-")
                + "-" + (Integer.parseInt(StringUtils.substringAfterLast(v2Data.value(), "-")) + 2);
            terser.set("/." + s, v2Data.system());
        }
    }

    @SuppressWarnings("unused")
    private int maxErrors = Validator.DEFAULT_MAX_ERRORS;

    private Set<String> suppressed;

    @SuppressWarnings("unused")
    private String version = Validator.DEFAULT_VERSION;

    private boolean isValidationEnabled = true;

    public Converter() {
    }

    public CVRSExtract convert() {
        return null;

    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors  = maxErrors;
    }
    public void setSuppressed(Collection<String> suppressErrors) {
        this.suppressed.clear();
        this.suppressed.addAll(suppressErrors);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setValidationEnabled(boolean isValidationEnabled) {
        this.isValidationEnabled = isValidationEnabled;
    }
    public boolean isValidationEnabled() {
        return isValidationEnabled;
    }
}
