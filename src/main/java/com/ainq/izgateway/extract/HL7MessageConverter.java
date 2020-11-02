package com.ainq.izgateway.extract;

import java.io.Reader;
import java.util.Iterator;

import com.opencsv.exceptions.CsvException;

import ca.uhn.hl7v2.model.Message;

public class HL7MessageConverter implements Iterable<CVRSExtract> {
    HL7MessageParser parser;
    public HL7MessageConverter(Reader r) {
        parser = new HL7MessageParser(r);
    }

    private class ConversionWrapper implements Iterator<CVRSExtract> {
        private Iterator<Message> base;
        private int count = 0;
        private ConversionWrapper(Iterator<Message> base) {
            this.base = base;
        }
        @Override
        public boolean hasNext() {
            return base.hasNext();
        }
        @Override
        public CVRSExtract next() {
            try {
                count++;
                Message msg = base.next();
                return Converter.fromHL7(msg, null, null, count);
            } catch (CsvException e) {
                // This shouldn't happen.
                throw new RuntimeException("Unexpected Exception", e);
            }
        }
    }

    @Override
    public Iterator<CVRSExtract> iterator() {
        return new ConversionWrapper(parser.iterator());
    }

}
