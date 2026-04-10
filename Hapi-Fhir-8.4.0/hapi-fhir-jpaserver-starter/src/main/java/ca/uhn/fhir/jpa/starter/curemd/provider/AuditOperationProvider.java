package ca.uhn.fhir.jpa.starter.curemd.provider;


import ca.uhn.fhir.jpa.starter.curemd.audit.AuditAttrs;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditQuery;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditService;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

import static ca.uhn.fhir.jpa.starter.curemd.audit.AuditDtos.AuditPageResponse;
import static ca.uhn.fhir.jpa.starter.curemd.audit.AuditDtos.AuditRow;

@Configuration
public class AuditOperationProvider {

	private static final Set<String> AUDIT_ALLOWED_PARAMS = Set.of("clientId", "userId", "username", "resourceType", "resourceId", "from", "to", "statusCode", "pageSize", "pageToken");
	private static final Set<String> AUDIT_COUNT_ALLOWED_PARAMS = Set.of("clientId", "userId", "username", "resourceType", "resourceId", "from", "to", "statusCode");
	private final AuditService auditService;

	public AuditOperationProvider(AuditService auditService) {
		this.auditService = auditService;
	}

	/**
	 * GET /fhir/$audit
	 */
	@Operation(name = "$audit", idempotent = true, manualResponse = false)
	public Bundle audit(
		RequestDetails requestDetails,

		@OperationParam(name = "clientId") String clientId,
		@OperationParam(name = "userId") String userId,
		@OperationParam(name = "username") String username,
		@OperationParam(name = "resourceType") String resourceType,
		@OperationParam(name = "resourceId") String resourceId,
		@OperationParam(name = "statusCode") String statusCode,
		@OperationParam(name = "from") String from,
		@OperationParam(name = "to") String to,

		@OperationParam(name = "pageSize") String pageSize,
		@OperationParam(name = "pageToken") String pageToken
	) {

		rejectUnknownParams(requestDetails, AUDIT_ALLOWED_PARAMS);

		Integer pageSizeInt = parsePageSize(pageSize);
		Integer statusCodeInt = parseStatusCode(statusCode);
		validateFromTo(from, to);
		try {
			String baseUrl = requestDetails.getFhirServerBase() + "/$audit";
			String tenant = safe(requestDetails.getAttribute(AuditAttrs.TENANT));

			AuditQuery q = new AuditQuery(tenant, clientId, userId, username, resourceType, resourceId, statusCodeInt, from, to);
			AuditPageResponse out = auditService.search(q, pageSizeInt, pageToken, baseUrl);

			// Return results as a FHIR Bundle (type=searchset) so paging links are natural
			Bundle bundle = new Bundle();
			bundle.setType(Bundle.BundleType.SEARCHSET);
			bundle.setTotal(out.items().size()); // optional; total count can be expensive so we don't compute it here

			for (AuditRow row : out.items()) {
				Parameters p = toParameters(row);
				BundleEntryComponent entry = bundle.addEntry();
				entry.setResource(p);
			}

			// Add paging link
			if (out.nextLink() != null) {
				bundle.addLink(new BundleLinkComponent().setRelation("next").setUrl(out.nextLink()));
			}

			return bundle;
		} catch (Exception e) {
			e.printStackTrace();
			throw new InternalErrorException("Unable to fetch audit records");
		}
	}

	/**
	 * GET /fhir/$audit-count
	 */
	@Operation(name = "$audit-count", idempotent = true)
	public Parameters auditCount(
		RequestDetails requestDetails,
		@OperationParam(name = "clientId") String clientId,
		@OperationParam(name = "userId") String userId,
		@OperationParam(name = "username") String username,
		@OperationParam(name = "resourceType") String resourceType,
		@OperationParam(name = "resourceId") String resourceId,
		@OperationParam(name = "statusCode") String statusCode,
		@OperationParam(name = "from") String from,
		@OperationParam(name = "to") String to
	) {
		rejectUnknownParams(requestDetails, AUDIT_COUNT_ALLOWED_PARAMS);

		Integer statusCodeInt = parseStatusCode(statusCode);
		validateFromTo(from, to);
		try {
			String tenant = safe(requestDetails.getAttribute(AuditAttrs.TENANT));
			AuditQuery q = new AuditQuery(tenant, clientId, userId, username, resourceType, resourceId, statusCodeInt, from, to);
			long count = auditService.count(q);

			Parameters params = new Parameters();
			params.addParameter().setName("count").setValue(new org.hl7.fhir.r4.model.IntegerType((int) count));
			return params;
		} catch (Exception e) {
			e.printStackTrace();
			throw new InternalErrorException("Unable to fetch audit count");
		}
	}

	private Parameters toParameters(AuditRow r) {
		Parameters p = new Parameters();

		add(p, "id", r.id());
		add(p, "eventTime", r.eventTime() != null ? r.eventTime().toString() : null);
		add(p, "httpMethod", r.httpMethod());
		add(p, "requestPath", r.requestPath());
		add(p, "operation", r.operation());
		add(p, "resourceType", r.resourceType());
		add(p, "resourceId", r.resourceId());
		add(p, "tenant", r.tenant());
		add(p, "clientId", r.clientId());
		add(p, "username", r.username());
		add(p, "statusCode", Integer.toString(r.statusCode()));
		add(p, "durationMs", Integer.toString(r.durationMs()));
		add(p, "errorClass", r.errorClass());
		add(p, "errorMessage", r.errorMessage());

		return p;
	}

	private void add(Parameters p, String name, String value) {
		if (value == null) return;
		p.addParameter().setName(name).setValue(new StringType(value));
	}

	@Nullable
	private String safe(@Nullable Object obj) {
		if (obj == null) return null;

		String s = obj.toString();
		if (s.isEmpty()) return null;

		final int MAX_LENGTH = 200;
		return s.length() <= MAX_LENGTH ? s : s.substring(0, MAX_LENGTH);
	}

	private void rejectUnknownParams(RequestDetails requestDetails, Set<String> allowed) {
		Map<String, String[]> params = requestDetails.getParameters();
		for (String key : params.keySet()) {
			if (!allowed.contains(key)) {
				throw new InvalidRequestException("Unknown parameter: " + key);
			}
		}
	}

	private Integer parsePageSize(String pageSize) {
		if (pageSize == null || pageSize.isBlank()) {
			return null; // let service apply default
		}
		int ps;
		try {
			ps = Integer.parseInt(pageSize);
		} catch (NumberFormatException e) {
			throw new InvalidRequestException("Invalid pageSize. Must be an integer.");
		}
		if (ps < 1) {
			throw new InvalidRequestException("Invalid pageSize. Must be >= 1.");
		}
		if (ps > 500) {
			throw new InvalidRequestException("Invalid pageSize. Max allowed is 500.");
		}
		return ps;
	}

	private Integer parseStatusCode(String statusCode) {
		if (statusCode == null || statusCode.isBlank()) {
			return null;
		}
		int sc;
		try {
			sc = Integer.parseInt(statusCode);
		} catch (NumberFormatException e) {
			throw new InvalidRequestException("Invalid statusCode. Must be an integer.");
		}
		if (sc < 100 || sc > 599) {
			throw new InvalidRequestException("Invalid statusCode. Must be between 100 and 599.");
		}
		return sc;
	}

	private void validateFromTo(String from, String to) {
		OffsetDateTime fromDt = parseDate(from, false);
		OffsetDateTime toDt = parseDate(to, true);

		if (fromDt != null && toDt != null && fromDt.isAfter(toDt)) {
			throw new InvalidRequestException("'from' must not be greater than 'to'.");
		}
	}

	private OffsetDateTime parseDate(String value, boolean isEnd) {
		if (value == null || value.isBlank()) return null;

		try {
			// Full datetime with timezone
			return OffsetDateTime.parse(value);
		} catch (DateTimeParseException e) {
			try {
				// Date-only (YYYY-MM-DD)
				LocalDate d = LocalDate.parse(value);
				return isEnd
					? d.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
					: d.atStartOfDay().atOffset(ZoneOffset.UTC);
			} catch (DateTimeParseException ex) {
				throw new InvalidRequestException("Invalid date format. Use ISO-8601 format (e.g. YYYY-MM-DD or YYYY-MM-DDThh:mm:ssZ).");
			}
		}
	}
}
