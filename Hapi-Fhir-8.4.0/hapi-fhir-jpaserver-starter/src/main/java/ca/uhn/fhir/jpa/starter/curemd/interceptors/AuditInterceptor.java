package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditService;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

public class AuditInterceptor {

	private final AuditService auditService;
	private static final Logger LOG = Logger.getLogger(AuditInterceptor.class);

	public AuditInterceptor(AuditService auditService) {
		this.auditService = auditService;
	}

	@Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
	public void completedNormally(RequestDetails rd, ServletRequestDetails srd) {
		try {
			HttpServletRequest req = srd.getServletRequest();
			HttpServletResponse resp = srd.getServletResponse();
			if (isAuditEndpoint(rd, srd)) {
				return;
			}

			String op = rd.getRestOperationType() != null ? rd.getRestOperationType().name() : "UNKNOWN";
			String resourceType = rd.getResourceName();
			String resourceId = rd.getId() != null ? rd.getId().getIdPart() : null;

			LOG.infof(
				"Audit (normal completion): method=%s, uri=%s, op=%s, resourceType=%s, resourceId=%s, status=%s",
				req.getMethod(),
				req.getRequestURI(),
				op,
				resourceType,
				resourceId,
				resp != null ? resp.getStatus() : null
			);

			auditService.write(req, resp, op, resourceType, resourceId, null);
		} catch (Exception ex) {
			LOG.error("Error in AuditInterceptor.completedNormally", ex);
		}
	}

	@Hook(Pointcut.STORAGE_PRE_INITIATE_BULK_EXPORT)
	public void preInitiateBulkExport(Object bulkParams, RequestDetails rd, ServletRequestDetails srd) {
		try {

			HttpServletRequest req = srd.getServletRequest();
			HttpServletResponse resp = srd.getServletResponse();
			if (isAuditEndpoint(rd, srd)) {
				return;
			}

			// Operation name for audit
			String op = rd.getRestOperationType() != null ? rd.getRestOperationType().name() : "UNKNOWN";

			// System-level export => rd.getResourceName() is usually null
			String resourceType = rd.getResourceName() != null ? rd.getResourceName() : "SYSTEM";

			String resourceId = (rd.getId() != null) ? rd.getId().getIdPart() : null;

			LOG.infof(
				"Audit (pre initiate bulk export): method=%s, uri=%s, op=%s, resourceType=%s, resourceId=%s",
				req.getMethod(),
				req.getRequestURI(),
				op,
				resourceType,
				resourceId
			);

			auditService.write(req, resp, op, resourceType, resourceId, null /* ex */);
		} catch (Exception ex) {
			LOG.error("Error in AuditInterceptor.preInitiateBulkExport", ex);
		}
	}

	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	public boolean handleException(RequestDetails rd,
											 ServletRequestDetails srd,
											 HttpServletRequest req,
											 HttpServletResponse resp,
											 BaseServerResponseException ex) {
		try {
			String op = rd.getRestOperationType() != null ? rd.getRestOperationType().name() : "UNKNOWN";
			String resourceType = rd.getResourceName();
			String resourceId = rd.getId() != null ? rd.getId().getIdPart() : null;

			LOG.infof(
				"Audit (exception): method=%s, uri=%s, op=%s, resourceType=%s, resourceId=%s, exception=%s",
				req.getMethod(),
				req.getRequestURI(),
				op,
				resourceType,
				resourceId,
				ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : null
			);

			auditService.write(req, resp, op, resourceType, resourceId, ex);
			return true;
		} catch (Exception auditEx) {
			LOG.error("Error in AuditInterceptor.handleException", auditEx);
			return false;
		}
	}

	private boolean isAuditEndpoint(RequestDetails rd, ServletRequestDetails srd) {
		String op = rd != null ? rd.getOperation() : null;
		if ("$audit".equals(op) || "audit-count".equals(op)) {
			return true;
		}

		// Optional: defensive guard based on URL path
		if (srd != null && srd.getServletRequest() != null) {
			String uri = srd.getServletRequest().getRequestURI();
			// adjust the substring to match your routing, e.g. "/fhir/$audit" etc.
			if (uri != null && uri.contains("$audit")) {
				return true;
			}
		}

		return false;
	}
}







