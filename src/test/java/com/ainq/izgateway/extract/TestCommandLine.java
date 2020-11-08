package com.ainq.izgateway.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


public class TestCommandLine {

    @ParameterizedTest
    @CsvSource( {
        "src/test/resources/testgood.txt,0,src/test/resources/testgood.txt.rpt",
        "src/test/resources/testgood.hl7,0,src/test/resources/testgood.hl7.rpt",
        "src/test/resources/testerror.txt,180,src/test/resources/testerror.txt.rpt",
        "src/test/resources/testerror.hl7,180,src/test/resources/testerror.hl7.rpt",
    })

    public void testCommandLine(String file, int errorCount, String baseline) throws IOException, InterruptedException {
        String command[] = { file};
        Path dir = Files.createTempDirectory("cvrs");
        String outputDir = dir.toFile().getCanonicalPath();
        int errors = Validator.main1(command, outputDir);
        assertEquals(errorCount, errors);

        File outputFile = Utility.getNewFile(file, dir.toFile(), "rpt");
        compareFiles(outputFile, new File(baseline), TestCommandLine::ignoreTomorrow);

        try {
            FileUtils.deleteDirectory(dir.toFile());
        } catch (IOException ioex) {
            // Swallow these, we don't really care.
        }

    }

    private void compareFiles(File actual, File expected, Function<String, String> conv) {
        // TODO Auto-generated method stub
        String actualContent = readFile(actual);
        String expectedContent = readFile(expected);
        if (conv != null) {
            actualContent = conv.apply(actualContent);
            expectedContent = conv.apply(expectedContent);
        }
        try {
            assertEquals(expectedContent, actualContent);
        } catch (Error err) {
            int line = 1, col = 1;
            String message = null;
            for (int i = 0; i < Math.min(expectedContent.length(), actualContent.length()); i++) {
                if (expectedContent.charAt(i) != actualContent.charAt(i)) {
                    message = String.format("Mismatch between %s and %s at Character %d (%d,%d): %d(%c) != %d(%c)%n",
                        expected, actual,
                        i, line, col,
                        (int)expectedContent.charAt(i), expectedContent.charAt(i),
                        (int)actualContent.charAt(i), actualContent.charAt(i));
                    throw new Error(message, err);
                }
                if (expectedContent.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col ++;
                }
            }
            throw err;
        }
    }

    @ParameterizedTest
    @CsvSource( {
        "src/test/resources/testgood.txt,src/test/resources/testgood.hl7,src/test/resources/testgood.cnv.rpt",
        "src/test/resources/testerror.txt,src/test/resources/testerror.hl7,src/test/resources/testerror.cnv.rpt",
    })
    public void testConversion(String file, String file2, String baseline) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("cvrs");
        String outputDir = dir.toFile().getCanonicalPath();
        String command[] = { "-sALL", (file.endsWith(".txt") ? "-7" : "-c") + outputDir, file };
        Validator.main1(command, outputDir);
        File outputFile = Utility.getNewFile(file, dir.toFile(), "hl7");

        // Verify Converted File Matches Baseline Conversion
        compareFiles(outputFile, new File(file2), TestCommandLine::cleanMSH);

        // Verify Report File Matches Baseline Report
        outputFile = Utility.getNewFile(file, dir.toFile(), "rpt");
        compareFiles(outputFile, new File(baseline), null);

        try {
            FileUtils.deleteDirectory(dir.toFile());
        } catch (IOException ioex) {
            // Swallow these, we don't really care.
        }

    }

    private static String readFile(File file) {
        try {
            // Change Windows CR-LF to just a LF so content comparison works
            // on Linux and Windows platforms
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8).replaceAll("[\\r\\n]+", "\n");
        } catch (FileNotFoundException fnex) {
            return String.format("%s not found.", file.getPath());
        } catch (IOException fnex) {
            return String.format("I/O Error reading %s.", file.getPath());
        }

    }

    private static String ignoreTomorrow(String contents) {
        return contents.replaceAll("tomorrow \\([0-9]{4}-[0-9]{2}-[0-9]{2}\\)", "tomorrow (DON'T CARE)");
    }
    private static String cleanMSH(String contents) {
        BufferedReader r = new BufferedReader(new StringReader(contents));
        StringBuffer b = new StringBuffer();
        String line = null;
        try {
            while ((line = r.readLine()) != null) {
                if (line.startsWith("MSH")) {
                    String fields[] = line.split("\\|");
                    if (fields.length > 6) {
                        fields[6] = "DONTCARE";
                    }
                    if (fields.length > 9) {
                        fields[9] = "DONTCARE";
                    }
                    line = StringUtils.join(fields, "|");
                }
                b.append(line).append('\n');
            }
        } catch (IOException e) {
           // This will never happen with a StringReader
        }
        if (b.length() > 0) {
            b.setCharAt(b.length()-1, '\r');
        }
        return b.toString();
    }

}
