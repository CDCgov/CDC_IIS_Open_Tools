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
