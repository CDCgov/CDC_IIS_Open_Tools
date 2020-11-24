package com.ainq.izgateway.clearinghouse.sender;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

public class GZipClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

    private long length = 0;
    public GZipClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
    }
    public GZipClientHttpRequestFactory(ClientHttpRequestFactory requestFactory, long length) {
        super(requestFactory);
        this.length = length;
    }

    @Override
    protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
            throws IOException {
        ClientHttpRequest delegate = requestFactory.createRequest(uri, httpMethod);
        ZippedClientHttpRequest zc = new ZippedClientHttpRequest(delegate, length);

        return zc;
    }
}