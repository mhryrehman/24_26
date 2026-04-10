package ca.uhn.fhir.jpa.starter.curemd;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.http.HttpServletResponse;

public class CustomRestfulServer extends RestfulServer {

	public CustomRestfulServer(FhirContext theCtx) {
		super(theCtx);
	}
	@Override
	public void addHeadersToResponse(HttpServletResponse theHttpResponse) {
		theHttpResponse.addHeader("X-Powered-By", "CuredMD FHIR Server");

		System.out.println("✅ addHeadersToResponse overridden: X-Powered-By suppressed");
	}
}
