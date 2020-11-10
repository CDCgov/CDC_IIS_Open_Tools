package com.ainq.izgateway.extract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Set;

import com.ainq.izgateway.extract.validation.BeanValidator;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.exceptions.CsvException;

public class ParserFactory extends CSVParser {
    public static class TooManyErrorsHandler implements CsvExceptionHandler {
        private int count = 0;
        private int maxErrors = Validator.DEFAULT_MAX_ERRORS;
        public TooManyErrorsHandler() {
        }

        public TooManyErrorsHandler(int max) {
            if (max == 0) {
                // Use the default value.
                return;
            }
            maxErrors = max;
        }
        @Override
        public CsvException handleException(CsvException e) throws CsvException {
            if (maxErrors == 1)
                throw e;
            if (maxErrors > 0 && count++ >= maxErrors) {
                throw new CsvException("Too many errors");
            }
            return e;
        }
    }

    public static CSVParser newCSVParser() {
        CSVParserBuilder b = new CSVParserBuilder();
        b.withIgnoreLeadingWhiteSpace(false)
         .withIgnoreQuotations(true)
         .withSeparator('\t')
         .withEscapeChar(NULL_CHARACTER)
         .withQuoteChar(NULL_CHARACTER)
         .withStrictQuotes(false);
        return b.build();
    }

    public static CSVReaderHeaderAware newReader(Reader r) {
        CSVReaderHeaderAwareBuilder b = new CSVReaderHeaderAwareBuilder(r);
        b.withCSVParser(newCSVParser())
         .withKeepCarriageReturn(false)
         .withMultilineLimit(1);
        return b.build();
    }

    public static CsvToBean<CVRSExtract> newTabDelimitedBeanReader(Reader r, BeanValidator validator, int maxErrors) {
        CsvToBeanBuilder<CVRSExtract> b = new CsvToBeanBuilder<CVRSExtract>(r);
        b.withIgnoreLeadingWhiteSpace(false)
         .withIgnoreQuotations(true)
         .withSeparator('\t')
         .withEscapeChar(NULL_CHARACTER)
         .withQuoteChar(NULL_CHARACTER)
         .withStrictQuotes(false)
         .withKeepCarriageReturn(false)
         .withMultilineLimit(1)
         .withType(CVRSExtract.class)
         .withExceptionHandler(new TooManyErrorsHandler(maxErrors))
         .withOrderedResults(true);

        // Add a verifier if requested.
        if (validator != null) {
            b.withVerifier(validator);
            for (Field f: BeanValidator.getIgnoredFields(validator.getVersion())) {
                b.withIgnoreField(CVRSExtract.class, f);
            }
        }
        return b.build();
    }

    public static CsvToBean<CVRSExtract> newCSVBeanReader(Reader r, BeanValidator validator, int maxErrors) {
        CsvToBeanBuilder<CVRSExtract> b = new CsvToBeanBuilder<CVRSExtract>(r);
        // Accept defaults for CSV Formatted files
        b.withKeepCarriageReturn(false)
         .withMultilineLimit(10)
         .withType(CVRSExtract.class)
         .withExceptionHandler(new TooManyErrorsHandler(maxErrors))
         .withOrderedResults(true);

        // Add a verifier if requested.
        if (validator != null) {
            b.withVerifier(validator);
            for (Field f: BeanValidator.getIgnoredFields(validator.getVersion())) {
                b.withIgnoreField(CVRSExtract.class, f);
            }
        }
        return b.build();
    }

    public static BeanValidator newVerifier(Set<String> suppressed, String version) {
        return new BeanValidator(suppressed, version);
    }

    public static Iterable<CVRSExtract> newParser(Reader r, BeanValidator validator, boolean useDefaults) throws IOException {
        BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
        br.mark(1024);
        String line = br.readLine();
        br.reset();
        if (HL7MessageParser.isMessageDelimiter(line)) {
            // This is an HL7 Message, create a parser for HL7
            return new HL7MessageConverter(br, useDefaults, validator);
        }
        if (line.contains("\t")) {
            return newTabDelimitedBeanReader(br, validator, 0);
        }
        return newCSVBeanReader(br, validator, 0);
    }
}
