package com.ainq.izgateway.extract;

import java.io.Reader;
import java.util.Iterator;

import com.ainq.izgateway.extract.validation.BeanValidator;
import com.opencsv.exceptions.CsvException;

import ca.uhn.hl7v2.model.Message;

public class HL7MessageConverter implements Iterable<CVRSExtract> {
    private HL7MessageParser parser;
    private boolean useDefaults = false;
    private BeanValidator validator = null;

    public HL7MessageConverter(Reader r, boolean useDefaults, BeanValidator validator) {
        parser = new HL7MessageParser(r);
        this.useDefaults = useDefaults;
        this.validator = validator;
    }

    public HL7MessageConverter(Reader r, boolean useDefaults) {
        this(r, useDefaults, null);
    }

    public HL7MessageConverter(Reader r, BeanValidator validator) {
        this(r, false, validator);
    }

    public HL7MessageConverter(Reader r) {
        this(r, false, null);
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
                return Converter.fromHL7(msg, null, validator, useDefaults, count);
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
