package com.ainq.izgateway.extract;
/*
 * Copyright 2020 Audiacious Inquiry, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.apache.commons.lang3.StringUtils;

/**
 * A utility class for CVRS Validation and Conversion
 * @author Keith W. Boone
 */
public class Utility {
    /**
     * Private constructor for this class containing static member functions.
     */
    private Utility() {
    }

    /**
     * Write a tab-delimited row to a writer, ensuring any tabs in data are changed to spaces.
     * @param w The writer to write to
     * @param values    The values to write
     * @throws IOException  If an error occured while writing.
     */
    public static void writeRow(Writer w, String[] values) throws IOException {
        w.write(StringUtils.defaultString(values[0]));
        for (int i = 1; i < values.length; i++) {
            w.write("\t");
            w.write(StringUtils.defaultString(values[i]).replace("\t", " "));
        }
        w.write("\n");
    }

    /**
     * Write a tab-delimited row to a stream, ensuring any tabs in data are changed to spaces.
     * @param w The writer to write to
     * @param values    The values to write
     */
    public static void printRow(PrintStream w, String[] values) {
        if (values.length > 0) {
            w.print(StringUtils.defaultString(values[0]));
            for (int i = 1; i < values.length; i++) {
                w.print("\t");
                w.print(StringUtils.defaultString(values[i]).replace("\t", " "));
            }
            w.println();
        }
    }

    /**
     * Read a line (usually of headers) from a reader, and restoring it
     * to the original position.
     *
     * @param r The reader to read.
     * @return  The list of tab-delimited values on the line.
     * @throws IOException  If an IO Error occured while reading.
     */
    public static String[] readHeaders(BufferedReader r) throws IOException {
        // We read into a fixed size buffer in case someone
        // sends data that doesn't have any new lines.  The CVRS
        // header currently fits into 640 bytes.
        r.mark(1024);
        char buffer[] = new char[1024];
        if (r.read(buffer) < 0) {
            return null;
        }
        // Strip Byte Order Mark if Present
        String header = new String(buffer);

        // Get the actual header line.
        header = StringUtils.substringBefore(header, "\n").replace("\r", "");
        r.reset();

        return header == null ? null : header.split("\\t");
    }

    /**
     * Check for a value other than NO in a field.
     *
     * @param value The field to check
     * @return  true if the value is other than no or an empty string.
     */
    public static boolean isTrue(String value) {
        // True implies the value is not empty and the value is NOT = NO.
        return !StringUtils.isEmpty(value) && !"NO".equalsIgnoreCase(value);
    }

    /**
     * If reader is already a BufferedReader, return it, otherwise wrap it in a
     * BufferedReader.
     *
     * @param reader    The reader to wrap.
     * @return  A BufferedReader
     */
    public static BufferedReader getBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader) {
            return (BufferedReader)reader;
        }
        return new BufferedReader(reader);
    }

    public static File getNewFile(String name, File outputFolder, String ext) {
        String file = StringUtils.substringBeforeLast(name, ".");
        if (".".equals(outputFolder.getPath())) {
            return new File(String.format("%s.%s", file, ext));
        }
        return new File(outputFolder, String.format("%s.%s", file, ext));
    }

    /**
     * Given two lines of data, compute the width of the selected column for printing a report.
     * @param headers   The headers
     * @param cols  The data columns
     * @param i The selected column to compute the width ofr
     * @return  A width value that will allow both rows to be printed fully.
     */
    public static int getColumnWidth(String[] headers, String[] cols, int i) {
        int width = 0;
        if (i < headers.length) {
            width = StringUtils.defaultString(headers[i]).length();
        }
        if (i < cols.length) {
            width = Math.max(width, StringUtils.defaultString(cols[i]).length());
        }
        return Math.max(4, width + 1);
    }

    /**
     * Print a tab-delimited report row for diagnostics
     * @param headers   The headers for the row
     * @param cols      The columns containing data for the row
     * @param header    The header to print data around (limits the data printed to the specified column
     * plus or minus two columns for context).  If null, prints the entire row.
     * @param out   The stream to print the report to.
     */
    public static void printRow(String[] headers, String[] cols, String header, PrintStream out) {
        StringBuffer l1 = new StringBuffer(), l2 = new StringBuffer();
        int headerPos = headers.length;
        if (header != null) {
            for (headerPos = 0; headerPos < headers.length; headerPos++) {
                if (header.equalsIgnoreCase(headers[headerPos])) {
                    break;
                }
            }
        }
        for (int i = 0; i < Math.max(headers == null ? 0 : headers.length, cols.length); i++) {
            int width = getColumnWidth(headers, cols, i);
            String format = "%-" + width + "s";
            if (headerPos == headers.length || i >= headerPos - 2 && i <= headerPos + 2) {
                l1.append(String.format(format, i < headers.length ? headers[i] : ""));
                l2.append(String.format(format, i < cols.length ? cols[i] : ""));
            }
        }
        out.println(l1.toString());
        out.println(l2.toString());
        out.println();
    }

    public static Reader getReader(String arg) throws IOException {
        return getBufferedReader(
            "-".equals(arg) ? new InputStreamReader(System.in)
                            : getFileReader(arg));
    }

    public static InputStreamReader getFileReader(String arg) throws FileNotFoundException, UnsupportedEncodingException {
       return new InputStreamReader(new FileInputStream(arg), "UTF-8");
    }

    public static PrintStream getPrintStream(File f2) throws FileNotFoundException, UnsupportedEncodingException {
        return new PrintStream(f2, "UTF-8");
    }

}
