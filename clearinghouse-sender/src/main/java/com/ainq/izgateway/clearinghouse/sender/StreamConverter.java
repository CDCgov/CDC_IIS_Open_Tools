package com.ainq.izgateway.clearinghouse.sender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class StreamConverter implements HttpMessageConverter<InputStream> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return true;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.ALL, MediaType.TEXT_PLAIN);
    }

    @Override
    public InputStream read(Class<? extends InputStream> clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    public void write(InputStream t, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        byte buffer[] = new byte[4096];
        int length = 0;
        boolean foundLf = false;
        OutputStream body = outputMessage.getBody();
        while ((length = t.read()) > 0) {
            if (length == '\r') {
                continue;
            }
            body.write(length);
        }
        while ((length = t.read(buffer)) > 0) {

            for (int i = 0; !foundLf && i < length; i++) {
                if (buffer[i] == '\n') {
                    foundLf = true;
                    break;
                }
                // Convert first line (header) to Upper Case
                buffer[i] = (byte) Character.toUpperCase(buffer[i]);
            }
            body.write(buffer, 0, length);
        }
        body.flush();
    }

}
