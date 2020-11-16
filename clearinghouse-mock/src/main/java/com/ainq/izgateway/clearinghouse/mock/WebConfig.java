package com.ainq.izgateway.clearinghouse.mock;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ainq.izgateway.extract.model.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.ainq.izgateway.clearinghouse")
public class WebConfig implements WebMvcConfigurer {
    public static class JOM implements GenericHttpMessageConverter<Object> {

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return mediaType == null || mediaType.getSubtype().endsWith("json");
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return mediaType == null || mediaType.getSubtype().endsWith("json");
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Arrays.asList(
                new MediaType("application", "json"),
                new MediaType("application", "*+json")
            );
        }

        @Override
        public TokenResponse read(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
            ObjectMapper m = new ObjectMapper();
            return m.readerFor(clazz).readValue(inputMessage.getBody());
        }

        @Override
        public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
            ObjectMapper m = new ObjectMapper();
            m.writerFor(t.getClass()).writeValue(outputMessage.getBody(), t);
        }

        @Override
        public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return canRead(contextClass, mediaType);
        }

        @Override
        public TokenResponse read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
            ObjectMapper m = new ObjectMapper();
            return m.readerFor(contextClass).readValue(inputMessage.getBody());
        }

        @Override
        public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
            return canWrite(clazz, mediaType);
        }

        @Override
        public void write(Object t, Type type, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
            ObjectMapper m = new ObjectMapper();
            m.writerFor(t.getClass()).writeValue(outputMessage.getBody(), t);

        }
    };
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JOM());
    }
}
