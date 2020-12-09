package com.ainq.izgateway.clearinghouse.mock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ainq.izgateway.extract.Utility;
import com.ainq.izgateway.extract.Validator;
import com.ainq.izgateway.extract.model.TokenRequest;
import com.ainq.izgateway.extract.model.TokenResponse;
import com.ainq.izgateway.extract.model.UploadResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@RestController
@RequestMapping("/v0")
public class APIController {
    @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Not Found")
    public static class InvalidTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidTypeException(String msg) {
            super(msg);
        }
    }

    @ResponseStatus(value=HttpStatus.UNAUTHORIZED, reason="Authentication Failure")
    public static class UnauthorizedException extends Exception {
        private static final long serialVersionUID = 1L;

        UnauthorizedException(String msg) {
            super(msg);
        }
    }
    Cache<String,TokenResponse> issued =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    private static final long TOKEN_DURATION = TimeUnit.HOURS.toMillis(1);

    @PostMapping(path="/token/gen", consumes="application/json", produces="application/json")
    public TokenResponse getToken(TokenRequest req) {
        TokenResponse resp = new TokenResponse();
        if (isValid(req)) {
            resp.token = UUID.randomUUID().toString();
            resp.expiration = System.currentTimeMillis() + TOKEN_DURATION;
        }
        issued.put(resp.token, resp);
        return resp;
    }
    private boolean isValid(TokenRequest req) {
        return true;
    }

    @PostMapping(path="/upload/{type}/batch", headers="Authorization", consumes="text/plain", produces="application/json")
    public UploadResponse upload(
        HttpServletRequest request,
        @PathVariable("type") String type,
        @RequestHeader("Authorization") String token
    ) throws InvalidTypeException, UnauthorizedException, IOException {

        if (issued.getIfPresent(token) == null) {
            throw new UnauthorizedException("Not Authorize");
        }
        switch (type) {
        case "cvrs":
        case "hl7":
            // We don't need to distinguish between the two
            // because the validator already does this?
            return uploadBody(request.getInputStream(), token);
        default:
            throw new InvalidTypeException("Not Found");
        }
    }

    private UploadResponse uploadBody(InputStream body, String token) throws IOException {
        UploadResponse response = new UploadResponse();
        // Create a validator configured to support medium [e.g., DCH only] or high level
        // [DCH + additional checks] data quality validation restrictions.

        File outputFolder = Files.createTempDirectory("cvrs").toFile();
        File inputData = copyBodyToFile(body, outputFolder);

        String files[] = { inputData.getAbsolutePath() };

        try {
            Validator.validateFiles(
                ".",    // Put the report in the same folder as the input
                Validator.DEFAULT_MAX_ERRORS,   // Use default for max errors
                Collections.emptySet(),   // Don't suppress any errors
                Validator.DEFAULT_VERSION,  // Use the default version
                false,  // Don't write invalid data during conversion (not really applicable)
                true,   // Do use JSON for the output report
                false,  // Don't use defaults for conversion (not really applicable)
                false,  // Don't apply any fixes
                false,  // Don't report statistics
                null,   // Where to put converted HL7 outputs (not applicable)
                null,   // Where to put converted CVRS outputs (not applicable)
                files   // The file to  convert
            );

            File f = Utility.getNewFile("cvrs.in", outputFolder, "rpt.json");
            response = translateResponse(f);
        } finally {
            FileUtils.deleteDirectory(outputFolder);
        }
        return response;
    }

    /**
     * Collect the errors from specified file and report as if the reporting system
     * was DCH.  Return the results the the caller.
     * @param f The file to collect results from
     * @return The upload response.
     * @throws IOException  If an IO Error occurs
     * @throws JsonMappingException If we didn't read the JSON right
     * @throws JsonParseException   If the JSON isn't correctly formatted
     */
    private UploadResponse translateResponse(File f) throws JsonParseException, JsonMappingException, IOException {
        UploadResponse resp = new UploadResponse();
        resp.id = UUID.randomUUID().toString();
        resp.truncated = false;
        ObjectMapper m = new ObjectMapper();
        JsonNode j = m.readTree(f);
        JsonNode a = j.get("detail");
        List<String> processingErrors = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        if (a != null) {
            for (JsonNode row: a) {
                if (row.get("code").asText().equals("BUSR013")) {
                    processingErrors.add(
                        String.format("VaxEventId=%s RecipId=%s message=%s",
                            row.get("vax_event_id").asText(),
                            "XXA",
                            row.get("message").asText()
                        )
                    );
                } else {
                    validationErrors.add(
                        String.format("line#%d field=%s message=%s",
                            row.get("line").asInt(),
                            row.get("field").asText(),
                            row.get("message").asText()
                        )
                    );
                }
            }
        }

        resp.processingErrors = processingErrors.toArray(new String[processingErrors.size()]);
        resp.validationErrors = validationErrors.toArray(new String[validationErrors.size()]);

        return resp;
    }
    private File copyBodyToFile(InputStream body, File outputFolder) throws FileNotFoundException, IOException {
        File outputFile = new File(outputFolder, "cvrs.in");
        try (FileOutputStream fout = new FileOutputStream(outputFile);) {
            IOUtils.copy(body, fout);
        }
        return outputFile;
    }

}
