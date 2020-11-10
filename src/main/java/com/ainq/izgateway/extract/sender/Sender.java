package com.ainq.izgateway.extract.sender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ainq.izgateway.extract.HL7MessageParser;
import com.ainq.izgateway.extract.model.TokenRequest;
import com.ainq.izgateway.extract.model.TokenResponse;
import com.ainq.izgateway.extract.model.UploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableWebMvc
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.ainq.izgateway.extract.sender"
})
public class Sender implements WebMvcConfigurer, CommandLineRunner {
    /** The API Endpoint for token generation   */
    private static final String TOKEN_GEN_URL = "v0/token/gen";
    /** The API Endpoint for uploading CVRS Batch Files */
    private static final String UPLOAD_CVRS_BATCH_URL = "v0/upload/cvrs/batch";
    /** The API Endpoint for uploading HL7 Batch Files */
    private static final String UPLOAD_HL7_BATCH_URL = "v0/upload/hl7/batch";

    /** The Streaming converter */
    private static final StreamConverter streamConverter = new StreamConverter();

    /** The clientID for obtaining the token
     * Call this application with clientId set in your environment
     * or using -DclientID=<i>clientID</i> to set the clientID value used to
     * send files.
     */
    @Value("${clientID}")
    private String clientID;
    /** The clientSecret to use for obtaining the token
     * Call this application with clientSecret set in your environment
     * or using -DclientSecret=<i>clientSecret</i> to set the clientSecret value used to
     * send files.
     */
    @Value("${clientSecret}")
    private String clientSecret;
    String scopes[] = { "UPLOAD" };

    /**
     * The base url. Defaults to the test environment.
     * Set URL on the command line using -Durl=<i>url</i> or send URL in your
     * environment in order to specify where files should be uploaded.
     */
    @Value("${url:https://covdch-dev.ohms.oracle.com/}")
    private String base;

    /**
     * The token to use for this session.
     */
    private TokenResponse token = null;

    /**
     * Application entrypoint for Spring Boot console client apps.
     * @param args  Program arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(Sender.class, args);
    }

    /**
     * Uploads files specified on the command line.
     */
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Nothing to do!");
        }
        String line = null;
        for (String arg: args) {
            // Examine the file to determine if
            // 1. It can be read?
            // 2. What type of data it contains
            try (FileReader r = new FileReader(arg, StandardCharsets.UTF_8)) {
                char data[] = new char[4096];
                int len = r.read(data);
                line = new String(data, 0, len);
            } catch (FileNotFoundException fnex) {
                System.err.printf("Cannot find %s%n", arg);
                continue;
            } catch (IOException ioex) {
                System.err.printf("Error reading %s%n", arg);
                continue;
            }
            UploadResponse resp = null;
            if (HL7MessageParser.isBatchDelimiter(line)) {
                resp = uploadHL7Batch(arg);
            } else {
                resp = uploadCVRSBatch(arg);
            }
            ObjectMapper m = new ObjectMapper();
            m.writeValue(System.out, resp);
        }
    }

    @Bean
    public StreamConverter streamConverter() {
        return new StreamConverter();
    }

    private @Value("${client.compression.enabled:true}") boolean compressionEnabled;

    public RestTemplate getRestTemplate() {
        RestTemplate rt;
        if (compressionEnabled) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setBufferRequestBody(false);
            ClientHttpRequestFactory gzipRequestFactory = new GZipClientHttpRequestFactory(requestFactory);
            rt = new RestTemplate(gzipRequestFactory);
        } else {
            rt = new RestTemplate();
        }
        rt.getMessageConverters().add(streamConverter);
        return rt;
    }

    private TokenResponse requestToken() {
        TokenRequest tr = new TokenRequest();
        tr.clientID = clientID;
        tr.clientSecret = clientSecret;
        tr.scopes = scopes;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TokenRequest> requestBody = new HttpEntity<>(tr, headers);

        TokenResponse token = sendPost(TOKEN_GEN_URL, requestBody, TokenResponse.class);

        if (token != null && token.token != null) {
            return token;
        } else {
            return null;
        }
    }

    private <R, B> R sendPost(String endpointUrl, HttpEntity<B> requestBody, Class<? extends R> clazz) {
        RestTemplate tokenTemplate = getRestTemplate();
        String url = String.format("%s%s%s", base, base.endsWith("/") ? "" : "/", TOKEN_GEN_URL);

        return tokenTemplate.postForObject(url, requestBody, clazz);
    }

    private UploadResponse uploadCVRSBatch(String file) throws FileNotFoundException {
        return uploadBatch(UPLOAD_CVRS_BATCH_URL, file);
    }

    private UploadResponse uploadHL7Batch(String file) throws FileNotFoundException {
        return uploadBatch(UPLOAD_HL7_BATCH_URL, file);
    }

    private UploadResponse uploadBatch(String endpointUrl, String file) throws FileNotFoundException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", getAuthorization());
        System.out.println("Token Obtained!");
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.TEXT_PLAIN);

        UploadResponse resp = sendPost(endpointUrl, new HttpEntity<>(new FileInputStream(file), headers), UploadResponse.class);
        return resp;
    }

    private String getAuthorization() {
        if (token == null || isExpired(token)) {
            token = requestToken();
        }
        return token.token;
    }

    private boolean isExpired(TokenResponse t) {
        long now = System.currentTimeMillis() / 1000;
        return now > t.expiration;
    }
}
