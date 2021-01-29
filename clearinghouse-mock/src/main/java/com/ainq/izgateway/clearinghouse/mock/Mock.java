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
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.ainq.izgateway.clearinghouse.mock"
})
public class Mock {
	public static void main(String[] args) {
		SpringApplication.run(Mock.class, args);
	}

	/**
	 * This method creates a Bean that ensures the Jetty Server
	 * can handle decompression of inbound requests.
	 * @return A JettyServletWebServerFactory that can decompress
	 * inbound requests.
	 */
	@Bean
	public JettyServletWebServerFactory jettyServletWebServerFactory() {
	    JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
	    factory.addServerCustomizers(server -> {
	        GzipHandler gzipHandler = new GzipHandler();
	        gzipHandler.setInflateBufferSize(1);
	        gzipHandler.setHandler(server.getHandler());

	        HandlerCollection handlerCollection = new HandlerCollection(gzipHandler);
	        server.setHandler(handlerCollection);
	    });
	    return factory;
	}
}
