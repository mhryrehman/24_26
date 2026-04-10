package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Improved and safer interceptor to modify OperationOutcome responses
 * while avoiding throwing exceptions from the interceptor itself.
 */
@Interceptor
public class CustomErrorResponseInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(CustomErrorResponseInterceptor.class);


	private void modifyDiagnostics(OperationOutcome outcome) {
		if (outcome == null || outcome.getIssue() == null) {
			return;
		}

		for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
			if (issue.hasDiagnostics()) {
				String diagnostics = issue.getDiagnostics();
				if (diagnostics != null && diagnostics.startsWith("HAPI-")) {
					diagnostics = diagnostics.replaceFirst("HAPI-\\d+:\\s*", "");
					// safe set
					issue.setDiagnostics(diagnostics);
				}
			}
		}
	}

	@Hook(Pointcut.SERVER_OUTGOING_FAILURE_OPERATIONOUTCOME)
	public void onOutgoingFailureOperationOutcome(RequestDetails requestDetails,
																 ServletRequestDetails servletRequestDetails,
																 IBaseOperationOutcome rawOutcome) {
		if (rawOutcome == null) {
			return;
		}

		try {
			// Only handle R4 OperationOutcome instances here; other FHIR versions can be added if needed.
			if (!(rawOutcome instanceof OperationOutcome outcome)) {
				return;
			}

			if (outcome.getIssue() == null || outcome.getIssue().isEmpty()) {
				return;
			}

			modifyDiagnostics(outcome);

		} catch (Throwable t) {
			LOG.warn("OutgoingFailureOutcomeInterceptor aborted; letting HAPI continue: {}", safeMessage(t));
		}
	}

	private static String safeMessage(Throwable t) {
		if (t == null) return "null";
		String m = t.getMessage();
		if (m == null || m.isEmpty()) m = t.getClass().getSimpleName();
		if (m.length() > 200) m = m.substring(0, 200) + " (truncated)";
		return m;
	}
}
