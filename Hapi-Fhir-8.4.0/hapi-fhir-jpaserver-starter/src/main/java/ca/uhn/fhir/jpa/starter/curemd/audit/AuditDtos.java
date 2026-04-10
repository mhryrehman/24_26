package ca.uhn.fhir.jpa.starter.curemd.audit;

import java.time.OffsetDateTime;
import java.util.List;

public final class AuditDtos {
	private AuditDtos() {
	}

	public record AuditRow(
		String id,
		OffsetDateTime eventTime,
		String httpMethod,
		String requestPath,
		String operation,
		String resourceType,
		String resourceId,
		String tenant,
		String requiredScope,
		String roles,
		String scopes,
		String clientId,
		String username,
		String userId,
		String sourceIp,
		String userAgent,
		int statusCode,
		int durationMs,
		String errorClass,
		String errorMessage
	) {
	}

	public record AuditPageResponse(
		List<AuditRow> items,
		int pageSize,
		String nextPageToken,
		String nextLink
	) {
	}

	public record CountResponse(long count) {
	}
}
