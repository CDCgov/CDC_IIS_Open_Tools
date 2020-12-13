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
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;

/**
 * This class implements a parser for HL7 Message and Batch files.
 *
 * @author Keith W. Boone
 *
 */
public class HL7MessageParser implements Iterable<Message> {

    /** Fixed HAPI Context to use for HL7 parsing */
    private static final HapiContext HAPI_CONTEXT = new DefaultHapiContext();

    /**
     * Creates an Iterator that will run through all the messages
     * that can be parsed by this parser.
     */
    private class MessageIterator implements Iterator<Message> {
        private Message next = null;
        private boolean finished = false;
        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (finished) {
                return false;
            }
            try {
                next = nextMessage();
            } catch (IOException | HL7Exception e) {
                next = null;
                finished = true;
                throw new RuntimeException("Exception parsing HL7 Message", e);
            }
            if (next == null) {
                finished = true;
            }
            return next != null;
        }

        @Override
        public Message next() {
            if (hasNext()) {
                Message result = next;
                next = null;
                return result;
            }
            throw new NoSuchElementException();
        }
    }

    /** HAPI Parser to use for HL7 parsing */
    private Parser parser = HAPI_CONTEXT.getGenericParser();

    /**
     * The number of messages read (regardless of whether they are valid)
     */
    private int msgCount = 0;
    /**
     * The number of lines (segments) read (regardless of whether they are valid)
     */
    private int lineCount = 0;
    /**
     * The buffered reader to use for reading messages.
     * This class uses a buffered reader to enable skipping of File and Batch Headers and Trailers
     * and to use MSH segments alone as delimiters between messages.
     */
    private BufferedReader br;

    private static final String HL7_BATCH_DELIMITER_SEGMENT = "^(FHS|BHS|BTS|FTS).*$";
    private static final String HL7_DELIMITER_SEGMENT = "^(FHS|BHS|BTS|FTS|MSH).*$";

    private static final Pattern HL7_BATCH_DELIMITER_PATTERN = Pattern.compile(HL7_BATCH_DELIMITER_SEGMENT);
    private static final Pattern HL7_DELIMITER_PATTERN = Pattern.compile(HL7_DELIMITER_SEGMENT);

    public static boolean isBatchDelimiter(String line) {
        return HL7_BATCH_DELIMITER_PATTERN.matcher(line).matches();
    }
    public static boolean isMessageDelimiter(String line) {
        return HL7_DELIMITER_PATTERN.matcher(line).matches();
    }

    // MAYBETODO: Create constructor based on InputStream to support reading
    // based on character set declared in the message.

    public HL7MessageParser(Reader r) {
        br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
    }

    public Message nextMessage() throws IOException, HL7Exception {
        StringBuffer b = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            lineCount++;
            if (HL7_BATCH_DELIMITER_PATTERN.matcher(line).matches()) {
                continue;
            }
            if (!line.startsWith("MSH")) {
                System.err.println("Missing Message Header (MHS) Segment at line " + lineCount);
                continue;
            }

            do {
                b.append(line).append("\r");
                br.mark(1024);
                lineCount++;
            } while ((line = br.readLine()) != null && !HL7_DELIMITER_PATTERN.matcher(line).matches());

            // Adjust counts.
            msgCount = getMsgCount() + 1;
            lineCount--;
            br.reset();

            Message hapiMsg = parser.parse(b.toString());
            return hapiMsg;
        }
        // No more to read
        return null;
    }

    /**
     * @return the msgCount
     */
    public int getMsgCount() {
        return msgCount;
    }

    /**
     * @return the lineCount
     */
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public Iterator<Message> iterator() {
        return new MessageIterator();
    }
}
