package com.ainq.izgateway.extract.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;


public class ZippedClientHttpRequest extends WrapperClientHttpRequest
{
    private GZIPOutputStream zip;
    private boolean isEnabled = true;
    private long compressMinSize = 1024;

    private long length = 0;
    public ZippedClientHttpRequest(ClientHttpRequest delegate, long length) {
        super(delegate);
        this.length = length;
        if (shouldCompress()) {
            delegate.getHeaders().add("Content-Encoding", "gzip");
            if (length != 0) {
                delegate.getHeaders().add("Content-Length", Long.toString(length));
            }
        }
    }

    public ZippedClientHttpRequest(ClientHttpRequest delegate) {
        this (delegate, 0);
    }

    /**
     * @return the isEnabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @param isEnabled the isEnabled to set
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * @return the compressMinSize
     */
    public long getCompressMinSize() {
        return compressMinSize;
    }

    /**
     * @param compressMinSize the compressMinSize to set
     */
    public void setCompressMinSize(long compressMinSize) {
        this.compressMinSize = compressMinSize;
    }

    private boolean shouldCompress() {
        return isEnabled && (length == 0 || length > compressMinSize);
    }

    @Override
    public OutputStream getBody() throws IOException {
        final OutputStream body = super.getBody();
        if (shouldCompress()) {
            zip = new GZIPOutputStream(body);
            return zip;
        }
        return body;
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        if (zip!=null) zip.close();
        return super.execute();
    }

}
