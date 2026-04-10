package ca.uhn.fhir.jpa.starter.curemd.service;

import ca.uhn.fhir.jpa.starter.curemd.interceptors.AuthResponse;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CcdaService {

	public record CcdaResult(HttpStatusCode statusCode, String contentType, byte[] bytes) {}


	private static final Logger log = LoggerFactory.getLogger(CcdaService.class);

	private final RestTemplate restTemplate;
	private final ExternalApiConfig props;

	public CcdaService(ExternalApiConfig props, RestTemplate restTemplate) {
		this.props = props;
		this.restTemplate = restTemplate;
	}


	/**
	 * Calls: {practiceUrl}/api/ccda?startDate=...&endDate=...
	 * Returns response "as-is".
	 */
	public CcdaResult fetchCcda(String tenantId, String start, String end) {
//		String startIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start);
//		String endIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(end);

		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("startDate", start);
		queryParams.add("endDate", end);

//		ResponseEntity<byte[]> resp = callPracticeGet(tenantId, "/ccda", qp, byte[].class);
		AuthResponse auth = fetchToken(tenantId);

		String url = buildUrl(auth.getPracticeUrl(), "/ccda", queryParams);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(auth.getAccessToken());
		headers.setAccept(MediaType.parseMediaTypes("*/*"));

		ResponseEntity<byte[]> resp = restTemplate.exchange(
			url,
			HttpMethod.GET,
			new HttpEntity<>(headers),
			byte[].class
		);
		log.info("CCDA fetch response: status={}, headers={}", resp.getStatusCode(), resp.getHeaders());

		if (!resp.getStatusCode().is2xxSuccessful()) {
			throw new InvalidRequestException("CCDA fetch failed. status=" + resp.getStatusCode());
		}

		String contentType = resp.getHeaders().getContentType() != null
			? resp.getHeaders().getContentType().toString()
			: "application/octet-stream";

		byte[] bytes = resp.getBody() != null ? resp.getBody() : new byte[0];
		return new CcdaResult(resp.getStatusCode(), contentType, bytes);
	}

	public AuthResponse fetchToken(String tenantId) {
		log.info("Fetching token from external auth service for tenantId={}", tenantId);
		validateConfig(tenantId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "x-www-form-urlencoded", java.nio.charset.StandardCharsets.UTF_8));
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", props.getClientId());
		body.add("client_secret", props.getClientSecret());
		body.add("grant_type", props.getGrantType());
		body.add("username", props.getUsername(tenantId));
		body.add("password", props.getPassword(tenantId));

		try {
			ResponseEntity<AuthResponse> resp = restTemplate.exchange(
				props.getAuthUrl(),
				HttpMethod.POST,
				new HttpEntity<>(body, headers),
				AuthResponse.class
			);
			log.info("Token response: status={}, body={}", resp.getStatusCode(), resp.getBody());
			return resp.getBody();
		} catch (HttpClientErrorException e) {
			log.error("Token call failed. status={}, responseHeaders={}, responseBody={}",
				e.getStatusCode(), e.getResponseHeaders(), e.getResponseBodyAsString());
			throw e;
		}
	}


	private void validateConfig(String tenantId) {
		if (isBlank(props.getAuthUrl())
			|| isBlank(props.getClientId())
			|| isBlank(props.getClientSecret())
			|| isBlank(props.getGrantType())
			|| isBlank(props.getUsername(tenantId))
			|| isBlank(props.getPassword(tenantId))) {
			throw new InvalidRequestException("External auth config is missing (curemd.external.*)");
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}


	private String buildUrl(String baseUrl, String path, MultiValueMap<String, String> qp) {
		if (isBlank(baseUrl)) throw new InvalidRequestException("practiceUrl is blank in token response");

		String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String p = (path == null) ? "" : path.trim();
		if (!p.isEmpty() && !p.startsWith("/")) p = "/" + p;

		UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base + p);
		if (qp != null && !qp.isEmpty()) b.queryParams(qp);

		return b.build(true).toUriString();
	}
}
