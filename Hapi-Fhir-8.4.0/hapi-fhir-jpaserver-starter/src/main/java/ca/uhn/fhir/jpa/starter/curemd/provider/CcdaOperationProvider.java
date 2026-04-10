package ca.uhn.fhir.jpa.starter.curemd.provider;

import ca.uhn.fhir.jpa.starter.curemd.audit.AuditAttrs;
import ca.uhn.fhir.jpa.starter.curemd.service.CcdaService;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

@Configuration
public class CcdaOperationProvider {

	private static final Set<String> CCDA_ALLOWED_PARAMS = Set.of("startDate", "endDate");

	private final CcdaService ccdaService;

	public CcdaOperationProvider(CcdaService ccdaService) {
		this.ccdaService = ccdaService;
	}

	/**
	 * GET /fhir/$ccda?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
	 * (or ISO datetime)
	 */
	@Operation(name = "$ccda", idempotent = true, manualResponse = false)
	public Parameters ccda(
		RequestDetails requestDetails,
		@OperationParam(name = "startDate") String startDate,
		@OperationParam(name = "endDate") String endDate
	) {
		rejectUnknownParams(requestDetails);

		OffsetDateTime start = parseDate(startDate, false, "startDate");
		OffsetDateTime end = parseDate(endDate, true, "endDate");

//		if (start == null || end == null) {
//			throw new InvalidRequestException("startDate and endDate are required.");
//		}
		if (start != null && end !=null && start.isAfter(end)) {
			throw new InvalidRequestException("'startDate' must not be greater than 'endDate'.");
		}

		// tenant if you store it on RequestDetails (similar to AuditAttrs.TENANT)
		String tenant = safe(requestDetails.getAttribute(AuditAttrs.TENANT));

		// Service returns CCDA bytes + content-type (xml) etc.
		CcdaService.CcdaResult result = ccdaService.fetchCcda(tenant, startDate, endDate);

		Parameters out = new Parameters();
		out.addParameter().setName("startDate").setValue(new StringType(start.toString()));
		out.addParameter().setName("endDate").setValue(new StringType(end.toString()));
		out.addParameter().setName("contentType").setValue(new StringType(result.contentType()));

		// Base64 in Parameters (FHIR will serialize appropriately)
		Base64BinaryType data = new Base64BinaryType();
		data.setValue(result.bytes());
		out.addParameter().setName("data").setValue(data);

		return out;
	}

	private void rejectUnknownParams(RequestDetails requestDetails) {
		Map<String, String[]> params = requestDetails.getParameters();
		for (String key : params.keySet()) {
			if (!CcdaOperationProvider.CCDA_ALLOWED_PARAMS.contains(key)) {
				throw new InvalidRequestException("Unknown parameter: " + key);
			}
		}
	}

	private OffsetDateTime parseDate(String value, boolean isEnd, String paramName) {
		if (value == null || value.isBlank()) return null;

		try {
			return OffsetDateTime.parse(value);
		} catch (DateTimeParseException e) {
			try {
				LocalDate d = LocalDate.parse(value);
				return isEnd
					? d.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
					: d.atStartOfDay().atOffset(ZoneOffset.UTC);
			} catch (DateTimeParseException ex) {
				throw new InvalidRequestException(
					"Invalid " + paramName + ". Use ISO-8601 (YYYY-MM-DD or YYYY-MM-DDThh:mm:ssZ)."
				);
			}
		}
	}

	@Nullable
	private String safe(@Nullable Object obj) {
		if (obj == null) return null;
		String s = obj.toString();
		if (s.isEmpty()) return null;
		return s.length() <= 200 ? s : s.substring(0, 200);
	}
}
