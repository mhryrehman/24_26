package ca.uhn.fhir.jpa.starter.curemd;

import jakarta.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Author: Yasir Rehman
 * Description:
 * This configuration class customizes the embedded Tomcat server used in the application.
 * - Allows relaxed path and query characters for special character handling.
 * - Removes the `X-Powered-By` header at the Tomcat level.
 */
@Configuration
public class TomcatConfig {

	/**
	 * Configures the embedded Tomcat server by:
	 * - Allowing relaxed path and query characters.
	 * - Removing the `X-Powered-By` header.
	 *
	 * @return a `WebServerFactoryCustomizer` that applies the custom configuration.
	 */
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customTomcatConfig() {
		return factory -> {
			// Customize Tomcat connectors (existing functionality)
			factory.addConnectorCustomizers(connector -> {
				connector.setProperty("relaxedPathChars", "|{}[]");
				connector.setProperty("relaxedQueryChars", "|{}[]");
			});

			// Customize Tomcat context to disable X-Powered-By
			factory.addContextCustomizers((Context context) -> {
				context.setUseHttpOnly(true);
				context.addParameter("xpoweredBy", "false");
			});

			// Add a Tomcat Valve to remove the header at the response level
			factory.addEngineValves(new RemoveXPoweredByValve());
		};
	}

	/**
	 * Custom Tomcat Valve that removes the `X-Powered-By` header from responses.
	 */
	public static class RemoveXPoweredByValve extends ValveBase {
		@Override
		public void invoke(Request request, Response response) throws IOException, ServletException {
			getNext().invoke(request, response);
			response.setHeader("X-Powered-By", null); // Remove the header
		}
	}
}
