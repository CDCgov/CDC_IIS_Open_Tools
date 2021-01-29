package com.ainq.izgateway.extract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
        checkHL7Message(line, id, message);
    }

    @ParameterizedTest
    @Disabled("For diagnosing encoding error code flow")
    @MethodSource("getHL7MessageEncodingTestSuite")
    public void testHL7MessageEncoding(Integer line, String id, String message) throws IOException, InterruptedException, HL7Exception, CVRSValidationException {
        checkHL7Message(line, id, message);
    }

    @Test
    public void testNoLeadingMSH() throws CVRSValidationException, HL7Exception, IOException, InterruptedException {
        String message = "MSH|^~\\&|VAMS|NH0032||NHIIS|20201220182805+0000||VXU^V04^VXU_V04|VAMS-20201220-VXU04-a0Zt0000005p4MiEAI|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS|NH0032\r" +
            "PID|||1||Last^Jos�^Middle||20150826|F||POL^^CDCREC|Address 1^Address 2^City^MI^48877^^^^26001|||||||||||POL^^CDCREC\r" +
            "ORC|||identified_encoding_msh_error\r" +
            "RXA|||20201004||902^^CVX||||||^^^Luna&999999^^1^^^80 Ottaowa Ave NW^^Grand Rapids^MI^49504^^^26081\r" +
            "RXR||CE|VXC8^Member of Special Risk Group^PHIN VS||UNK\r" +
            "OBX||CE|59783-1^Status in immunization series^LN||No\r" +
            "OBX||CE|30973-2^Dose number in series^LN||1^^DCHDOSE\r" +
            "OBX||CE|75505-8^Serological Evidence of Immunity^LN||UNK^Unknown^NullFlavor";
        checkHL7Message(1, "identified_encoding_msh_error", message);
    }

    private void checkHL7Message(Integer line, String id, String message) throws IOException, InterruptedException, HL7Exception, CVRSValidationException {
        HL7MessageParser p = new HL7MessageParser(new StringReader(message));
        Message m = p.nextMessage();
        CVRSExtract ainqExtract = Converter.fromHL7(m, new ArrayList<>(), null, line);
        try {
            validator.verifyBean(ainqExtract);
            assertTrue(!StringUtils.isEmpty(ainqExtract.getRecip_address_street_2()), "recip_street_address2 must not be empty.");
            assertTrue(!StringUtils.isEmpty(ainqExtract.getRecip_middle_name()), "recip_middle_name must not be empty.");
            assertEquals(id.startsWith("identified_"), "I".equalsIgnoreCase(ainqExtract.getExt_type()));
            assertEquals(!id.startsWith("identified_"), "D".equalsIgnoreCase(ainqExtract.getExt_type()));
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
        return  getHL7MessagesFromFiles("src/test/resources/testdefault.hl7");
    }
    public static Stream<Object[]> getHL7MessageEncodingTestSuite() throws IOException {
        return  getHL7MessagesFromFiles("src/test/resources/testencoding.hl7");
    }

    private static Stream<Object[]> getHL7MessagesFromFiles(String ... files) throws IOException {
        List<Object[]> s = new ArrayList<>();
        for (String file: files) {
            String messages = FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8).replaceAll("[\r\n]+", "\n");
            String msg[] = messages.split("MSH\\|");
            int count = 0;
            for (String message: msg) {
                if (message.length() == 0) {
                    continue;
                }
                String id = StringUtils.substringBetween(message, "ORC|||", "\n");
                Object item[] = { Integer.valueOf(++count), id, "MSH|" + message };
                s.add(item);
            }
        }
        return s.stream();
    }
}
