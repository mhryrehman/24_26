package ca.uhn.fhir.jpa.starter.curemd.audit;

public record AuditQuery(
	String tenant,
	String clientId,
	String userId,
	String username,
	String resourceType,
	String resourceId,
	Integer statusCode,
	String from,   // ISO-8601 e.g. 2026-01-01T00:00:00Z
	String to      // ISO-8601 e.g. 2026-01-06T23:59:59Z
) {
}

