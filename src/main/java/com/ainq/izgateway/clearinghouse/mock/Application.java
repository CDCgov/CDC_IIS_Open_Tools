package com.ainq.izgateway.clearinghouse.mock;

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
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
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
