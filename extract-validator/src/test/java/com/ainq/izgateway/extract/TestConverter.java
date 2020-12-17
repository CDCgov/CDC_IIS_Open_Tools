package com.ainq.izgateway.extract;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.CVRSValidationException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;


public class TestConverter {

    private static BeanValidator validator = new BeanValidator(null, Validator.DEFAULT_VERSION, false);
    @ParameterizedTest
    @MethodSource("getHL7MessageDefaultTestSuite")
    public void testHL7MessageWithDefaults(Integer line, String id, String message) throws IOException, InterruptedException, HL7Exception, CVRSValidationException {
        HL7MessageParser p = new HL7MessageParser(new StringReader(message));
        Message m = p.nextMessage();
        CVRSExtract ainqExtract = Converter.fromHL7(m, new ArrayList<>(), null, line);
        try {
            validator.verifyBean(ainqExtract);
        } catch (CVRSValidationException e) {
            if (id.endsWith("_error")) {
                return;
            } else {
                throw e;
            }
        }
        if (id.endsWith("_error")) {
            assertTrue(false, "CVRS Validation Exception expected");
        }
    }

    public static Stream<Object[]> getHL7MessageDefaultTestSuite() throws IOException {
        String file = "src/test/resources/testdefault.hl7";
        String messages = FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8).replaceAll("[\r\n]+", "\n");
        String msg[] = messages.split("MSH\\|");
        List<Object[]> s = new ArrayList<>();
        int count = 0;
        for (String message: msg) {
            if (message.length() == 0) {
                continue;
            }
            String id = StringUtils.substringBetween(message, "ORC|||", "\n");
            Object item[] = { Integer.valueOf(++count), id, "MSH|" + message };
            s.add(item);
        }
        return s.stream();
    }
}
