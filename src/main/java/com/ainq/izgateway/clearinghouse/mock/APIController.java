package com.ainq.izgateway.clearinghouse.mock;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ainq.izgateway.extract.model.TokenRequest;
import com.ainq.izgateway.extract.model.TokenResponse;
import com.ainq.izgateway.extract.model.UploadResponse;
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
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).softValues().weakKeys().build();
    private static final long TOKEN_DURATION = TimeUnit.HOURS.toMillis(1);

    @PostMapping(path="/token/get", consumes="application/json", produces="application/json")
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
        @RequestBody InputStream body,
        @PathVariable("type") String type,
        @RequestHeader("Authorization") String token
    ) throws InvalidTypeException, UnauthorizedException {

        if (issued.getIfPresent(token) == null) {
            throw new UnauthorizedException("Not Authorize");
        }
        switch (type) {
        case "cvrs":
        case "hl7":
            // We don't need to distinguish between the two
            // because the validator already does this?
            return uploadBody(body, token);
        default:
            throw new InvalidTypeException("Not Found");
        }
    }

    private UploadResponse uploadBody(InputStream body, String token) {
        UploadResponse response = new UploadResponse();
        // Create a validator configured to support medium [e.g., DCH only] or high level
        // [DCH + additional checks] data quality validation restrictions.

        // Collect the errors and report as if the reporting system
        // was DCH.  Return the results the the caller.
        return response;
    }

}
