package com.ainq.izgateway.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestCommandLine {

    @ParameterizedTest
    @CsvSource( {
        "src/test/resources/testgood.txt,0",
        "src/test/resources/testgood.hl7,0",
        "src/test/resources/testerror.txt,178",
        "src/test/resources/testerror.hl7,176",
    })

    public void testCommandLine(String file, int errorCount) throws IOException, InterruptedException {
        String command[] = { file };
        int errors = Validator.main1(command);
        assertEquals(errorCount, errors);
    }
}
