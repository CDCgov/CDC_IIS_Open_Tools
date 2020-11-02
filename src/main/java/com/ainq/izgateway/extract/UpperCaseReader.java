package com.ainq.izgateway.extract;

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
