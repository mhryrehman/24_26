package ca.uhn.fhir.jpa.starter.curemd.audit;

import ca.uhn.fhir.jpa.starter.curemd.security.Constants;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@Order
public class ExportPollStatusAuditFilter extends OncePerRequestFilter {

	private static final Logger LOG = Logger.getLogger(ExportPollStatusAuditFilter.class);

	private final AuditService auditService;
	private final JsonFactory jsonFactory = new JsonFactory();

	public ExportPollStatusAuditFilter(AuditService auditService) {
		this.auditService = auditService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri == null) {
			LOG.trace("Poll-status audit filter skipped: URI is null");
			return true;
		}

		return !uri.contains(Constants.EXPORT_POLL_STATUS);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain)
		throws IOException {

		ContentCachingResponseWrapper wrapped =
			new ContentCachingResponseWrapper(response);

		try {
			filterChain.doFilter(request, wrapped);

			int status = wrapped.getStatus();
			if (status < 200 || status >= 300) {
				LOG.debugf("Poll-status audit skipped due to non-success status=%d uri=%s", status, request.getRequestURI());
				return;
			}

			byte[] body = wrapped.getContentAsByteArray();
			if (body.length == 0) {
				LOG.debug("Poll-status audit skipped: empty response body");
				return;
			}

			Set<String> resourceTypes = extractDistinctOutputTypes(body);

			if (resourceTypes.isEmpty()) {
				LOG.debug("Poll-status audit skipped: no output resource types found");
				return;
			}

			LOG.infof("Poll-status export completed. Logging audit entries for resourceTypes=%s", resourceTypes);
			request.removeAttribute(AuditAttrs.WRITTEN);
			auditService.writeExportPollStatusBatch(
				request,
				wrapped,
				resourceTypes,
				Constants.EXPORT_POLL_STATUS,
				null
			);

		} catch (Exception ex) {
			LOG.warn("Error while auditing export poll-status response", ex);
			auditService.write(
				request,
				wrapped,
				Constants.EXPORT_POLL_STATUS,
				Constants.EXPORT_POLL_STATUS,
				null,
				ex
			);
		} finally {
			wrapped.copyBodyToResponse();
		}
	}

	private Set<String> extractDistinctOutputTypes(byte[] body) {
		Set<String> out = new HashSet<>();

		try (JsonParser parser = jsonFactory.createParser(body)) {

			boolean inOutputArray = false;
			boolean expectingTypeValue = false;

			while (!parser.isClosed()) {
				JsonToken token = parser.nextToken();
				if (token == null) break;

				if (token == JsonToken.FIELD_NAME) {
					String fieldName = parser.getCurrentName();

					if (!inOutputArray && "output".equals(fieldName)) {
						token = parser.nextToken();
						if (token == JsonToken.START_ARRAY) {
							inOutputArray = true;
							LOG.trace("Entered export output array");
						}
						continue;
					}

					if (inOutputArray && "type".equals(fieldName)) {
						expectingTypeValue = true;
						continue;
					}
				}

				if (expectingTypeValue) {
					if (token == JsonToken.VALUE_STRING) {
						String type = parser.getValueAsString();
						if (type != null && !type.isBlank()) {
							out.add(type);
							LOG.tracef("Detected export output resource type: %s", type);
						}
					}
					expectingTypeValue = false;
				}

				if (inOutputArray && token == JsonToken.END_ARRAY) {
					LOG.trace("Exited export output array");
					break;
				}
			}

		} catch (Exception ex) {
			LOG.warn("Failed to parse export poll-status response JSON", ex);
		}

		return out;
	}
}
