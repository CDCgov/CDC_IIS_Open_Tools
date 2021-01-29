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
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class UpperCaseReader extends FilterReader {

    protected UpperCaseReader(Reader in) {
        super(in instanceof BufferedReader ? in : new BufferedReader(in));
    }

    /**
     * Reads a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        int c = in.read();
        if (c < 0) return c;
        return Character.toUpperCase(c);
    }

    /**
     * Reads characters into a portion of an array.
     *
     * @exception  IOException  If an I/O error occurs
     * @exception  IndexOutOfBoundsException {@inheritDoc}
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        int count = in.read(cbuf, off, len);
        for (int i = 0; i < count; i++) {
            cbuf[i + off] = Character.toUpperCase(cbuf[i + off]);
        }
        return count;
    }

    public static UpperCaseReader getReader(Reader r) {
        if (r instanceof UpperCaseReader) {
            return (UpperCaseReader) r;
        }
        return new UpperCaseReader(r);
    }
}
