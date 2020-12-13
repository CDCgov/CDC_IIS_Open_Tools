package com.ainq.izgateway.clearinghouse.mock;
/*
 * Copyright 2020 Audiacious Inquiry, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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
