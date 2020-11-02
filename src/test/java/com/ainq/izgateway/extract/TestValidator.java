package com.ainq.izgateway.extract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSValidationException;

import gov.nist.validation.report.Entry;

class TestValidator {

    @ParameterizedTest(name = "[{index}] -> {0}")
    @MethodSource("testErrorDetectionSource")
    void testErrorDetection(String test, String[] headers, String data[]) throws IOException {
        String code = test.split("_")[0];
        String rest = test.substring(code.length() + 1);
        Set<String> reported = new TreeSet<>();

        boolean found = false;
        try {
            runOne(test, headers, data);
        } catch (CVRSValidationException e) {
            for (List<Entry> entry: e.getReport().getEntries().values()) {
                if (entry.stream()
                    .filter(ee ->
                        {   reported.add(
                            String.format("%s(%s)", ee.getCategory(), ee.getPath()));
                            return true;
                        }
                    )
                    .anyMatch(ee ->
                        code.equals(ee.getCategory()) &&
                        rest.startsWith(ee.getPath())
                    )
                 ) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, code + " not reported for "  + test + ", found: " + reported);
    }

    private static CVRSExtract runOne(String test, String headers[], String data[]) throws CVRSValidationException, IOException {
        StringBuilder b = new StringBuilder();
        writeTabDelimited(b, headers);
        writeTabDelimited(b, data);
        BeanValidator bv = new BeanValidator(null, Validator.DEFAULT_VERSION);
        // stash this as a dup to enable duplicate checking
        bv.getEventIds().put("BUSR013_vax_event_id_nodups".toUpperCase(), "0");
        try (Validator v = new Validator(new StringReader(b.toString()), bv);) {
            v.setName(test);
            return v.validateOne();
        }
    }

    private static void writeTabDelimited(StringBuilder b, String fields[]) {
        for (String field: fields) {
            if (field != null) {
                b.append(field);
            }
            b.append("\t");
        }
        b.setCharAt(b.length()-1, '\n');
    }

    static Stream<Object[]> testErrorDetectionSource() throws IOException {
        return getTestResource("testerror.txt");
    }

    private static Stream<Object[]> getTestResource(String name) throws IOException {
        InputStream is = TestValidator.class.getClassLoader().getResourceAsStream(name);
        List<Object[]> o = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));) {
            String headers[];
            String line;
            line = r.readLine();
            headers = line.split("\\t");
            while ((line = r.readLine()) != null) {
                String data[] = line.split("\\t");
                // For some reason, Eclipse text editor removes trailing whitespace
                // from a line, which means that some test data drops fields if edited
                // in Eclipse. This ensures the number of returned fields is correct.
                if (data.length < headers.length) {
                    data = Arrays.copyOf(data, headers.length);
                }
                Object[] a = { data[0], headers, data };
                o.add(a);
            }
        }
        return o.stream();
    }

    @ParameterizedTest(name = "[{index}] -> {0}")
    @MethodSource("testGoodSamplesSource")
    void testGoodSamples(String test, String[] headers, String data[]) throws IOException {
        String code = test.split("_")[0];
        Set<String> reported = new TreeSet<>();

        boolean found = false;
        try {
            runOne(test, headers, data);
        } catch (CVRSValidationException e) {
            found = true;
            for (List<Entry> entry: e.getReport().getEntries().values()) {
                entry.forEach(ee ->
                        {   reported.add(
                            String.format("\n\t%s(%s): %s", ee.getCategory(), ee.getPath(), ee.getDescription()));
                        }
                    );
            }
        }
        assertFalse(found, code + " found errors: " + reported);
    }

    static Stream<Object[]> testGoodSamplesSource() throws IOException {
        return getTestResource("testgood.txt");
    }

}
