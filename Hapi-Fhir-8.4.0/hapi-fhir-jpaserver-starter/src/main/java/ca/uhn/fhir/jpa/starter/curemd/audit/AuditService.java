package ca.uhn.fhir.jpa.starter.curemd.audit;

import ca.uhn.fhir.jpa.starter.curemd.audit.AuditDtos.AuditPageResponse;
import ca.uhn.fhir.jpa.starter.curemd.audit.AuditDtos.AuditRow;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditService {
	private static final Logger LOG = Logger.getLogger(AuditService.class);

	public static final int DEFAULT_PAGE_SIZE = 100;
	public static final int MAX_PAGE_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	public AuditService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void write(HttpServletRequest req,
							HttpServletResponse resp,
							String operation,
							String resourceType,
							String resourceId,
							Exception ex) {

		LOG.tracef("Audit write invoked: operation=%s, resourceType=%s, resourceId=%s",
			operation, resourceType, resourceId);
		// allow interceptor to fetch user if filter didn't set
		AuditAttrs.tryPopulateFromSecurityContextIfMissing(req);

		// prevent duplicates if both filter + interceptor log
		if (Boolean.TRUE.equals(req.getAttribute(AuditAttrs.WRITTEN))) return;
		req.setAttribute(AuditAttrs.WRITTEN, true);

		int status;
		if (ex instanceof BaseServerResponseException bsrEx) {
			status = bsrEx.getStatusCode();
		} else {
			status = (resp != null ? resp.getStatus() : 500);
		}

		int durationMs = 0;
		Object start = req.getAttribute(AuditAttrs.START_NANO);
		if (start instanceof Long) {
			durationMs = (int) ((System.nanoTime() - (Long) start) / 1_000_000L);
		}

		String method = safe(req.getMethod(), 10);
		String path = safe(req.getRequestURI(), 2048);
		String query = safe(req.getQueryString(), 4096);

		String tenant = safe((String) req.getAttribute(AuditAttrs.TENANT), 200);
		String requiredScope = safe((String) req.getAttribute(AuditAttrs.REQUIRED_SCOPE), 200);
		String roles = safe((String) req.getAttribute(AuditAttrs.ROLES), 4000);
		String scopes = safe((String) req.getAttribute(AuditAttrs.SCOPES), 4000);
		String username = safe((String) req.getAttribute(AuditAttrs.USERNAME), 256);
		String userId = safe((String) req.getAttribute(AuditAttrs.USER_ID), 256);
		String clientId = safe((String) req.getAttribute(AuditAttrs.CLIENT_ID), 256);

		String ip = safe(req.getRemoteAddr(), 64);
		String ua = safe(req.getHeader("User-Agent"), 512);

		String op = safe(StringUtils.hasText(operation) ? operation : "UNKNOWN", 50);
		String rType = safe(resourceType, 100);
		String rId = safe(resourceId, 200);

		String errorClass = ex != null ? safe(ex.getClass().getName(), 256) : null;
		String errorMessage = ex != null ? safe(ex.getMessage(), 2000) : null;

		jdbcTemplate.update("""
					INSERT INTO fhir_audit_log
					(id, event_time, http_method, request_path, query_string,
					 operation, resource_type, resource_id,
					 tenant, required_scope, roles, scopes, username, user_id, client_id,
					 source_ip, user_agent, status_code, duration_ms,
					 error_class, error_message)
					VALUES
					(?, ?, ?, ?, ?,
					 ?, ?, ?,
					 ?, ?, ?, ?, ?, ?, ?,
					 ?, ?, ?, ?,
					 ?, ?)
				""",
			UUID.randomUUID(),
			Timestamp.from(OffsetDateTime.now().toInstant()),
			method, path, query,
			op, rType, rId,
			tenant, requiredScope, roles, scopes, username, userId, clientId,
			ip, ua, status, durationMs,
			errorClass, errorMessage
		);
	}

	public void writeExportPollStatusBatch(HttpServletRequest req,
														HttpServletResponse resp,
														Set<String> resourceTypes,
														String operation,
														Exception ex) {

		if (resourceTypes == null || resourceTypes.isEmpty()) return;

		AuditAttrs.tryPopulateFromSecurityContextIfMissing(req);

		if (Boolean.TRUE.equals(req.getAttribute(AuditAttrs.WRITTEN))) return;
		req.setAttribute(AuditAttrs.WRITTEN, true);

		// We still want to avoid duplicates for "normal" logging,
		// but this method is intentionally multi-row, so do not use WRITTEN here.

		int status = (resp != null ? resp.getStatus() : 500);

		int durationMs = 0;
		Object start = req.getAttribute(AuditAttrs.START_NANO);
		if (start instanceof Long) {
			durationMs = (int) ((System.nanoTime() - (Long) start) / 1_000_000L);
		}

		String method = safe(req.getMethod(), 10);
		String path = safe(req.getRequestURI(), 2048);
		String query = safe(req.getQueryString(), 4096);

		String tenant = safe((String) req.getAttribute(AuditAttrs.TENANT), 200);
		String requiredScope = safe((String) req.getAttribute(AuditAttrs.REQUIRED_SCOPE), 200);
		String roles = safe((String) req.getAttribute(AuditAttrs.ROLES), 4000);
		String scopes = safe((String) req.getAttribute(AuditAttrs.SCOPES), 4000);
		String username = safe((String) req.getAttribute(AuditAttrs.USERNAME), 256);
		String userId = safe((String) req.getAttribute(AuditAttrs.USER_ID), 256);
		String clientId = safe((String) req.getAttribute(AuditAttrs.CLIENT_ID), 256);

		String ip = safe(req.getRemoteAddr(), 64);
		String ua = safe(req.getHeader("User-Agent"), 512);

		String op = safe(StringUtils.hasText(operation) ? operation : "UNKNOWN", 50);

		String errorClass = ex != null ? safe(ex.getClass().getName(), 256) : null;
		String errorMessage = ex != null ? safe(ex.getMessage(), 2000) : null;
		// Prepare batch args
		List<Object[]> batchArgs = new ArrayList<>(resourceTypes.size());
		Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());

		for (String rt : resourceTypes) {
			String rType = safe(rt, 100);

			batchArgs.add(new Object[]{
				UUID.randomUUID(),
				now,
				method, path, query,
				op, rType, null,
				tenant, requiredScope, roles, scopes, username, userId, clientId,
				ip, ua, status, durationMs,
				errorClass, errorMessage
			});
		}

		jdbcTemplate.batchUpdate("""
			INSERT INTO fhir_audit_log
			(id, event_time, http_method, request_path, query_string,
			 operation, resource_type, resource_id,
			 tenant, required_scope, roles, scopes, username, user_id, client_id,
			 source_ip, user_agent, status_code, duration_ms,
			 error_class, error_message)
			VALUES
			(?, ?, ?, ?, ?,
			 ?, ?, ?,
			 ?, ?, ?, ?, ?, ?, ?,
			 ?, ?, ?, ?,
			 ?, ?)
			""", batchArgs);
	}

	/* =========================
	   READ
	   ========================= */

	public AuditPageResponse search(AuditQuery q, Integer pageSize, String pageToken, String baseUrl) {
		int size = sanitizePageSize(pageSize);
		LOG.infof("Audit search invoked: tenant=%s userId=%s resourceType=%s pageSize=%d",
			q.tenant(), q.userId(), q.resourceType(), size);
		// fetch size+1 to detect hasNext
		List<AuditRow> rows = queryPage(q, size + 1, pageToken);

		boolean hasNext = rows.size() > size;
		if (hasNext) rows = rows.subList(0, size);

		String nextToken = null;
		String nextLink = null;

		if (hasNext && !rows.isEmpty()) {
			AuditRow last = rows.get(rows.size() - 1);
			nextToken = Cursor.of(last).encode();
			nextLink = buildNextLink(baseUrl, q, size, nextToken);
		}

		return new AuditPageResponse(rows, size, nextToken, nextLink);
	}

	public long count(AuditQuery q) {
		LOG.infof("Audit count invoked: tenant=%s userId=%s resourceType=%s",
			q.tenant(), q.userId(), q.resourceType());

		StringBuilder sql = new StringBuilder("""
				SELECT COUNT(*)
				FROM fhir_audit_log
				WHERE 1=1
			""");
		List<Object> args = new ArrayList<>();
		addFilters(sql, args, q);

		Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
		return c != null ? c : 0L;
	}

	private List<AuditRow> queryPage(AuditQuery q, int limit, String pageToken) {
		StringBuilder sql = new StringBuilder("""
				SELECT *
				FROM fhir_audit_log
				WHERE 1=1
			""");
		List<Object> args = new ArrayList<>();

		addFilters(sql, args, q);

		// keyset paging (DESC)
		if (StringUtils.hasText(pageToken)) {
			Cursor c = Cursor.parse(pageToken);
			sql.append(" AND (event_time < ? OR (event_time = ? AND id < ?)) ");
			args.add(Timestamp.from(c.eventTime.toInstant()));
			args.add(Timestamp.from(c.eventTime.toInstant()));
			args.add(UUID.fromString(c.id));
		}

		sql.append(" ORDER BY event_time DESC, id DESC ");
		sql.append(" LIMIT ? ");
		args.add(limit);

		return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
	}

	private static void addFilters(StringBuilder sql, List<Object> args, AuditQuery q) {
		if (StringUtils.hasText(q.tenant())) {
			sql.append(" AND tenant = ? ");
			args.add(q.tenant());
		}
		if (StringUtils.hasText(q.clientId())) {
			sql.append(" AND client_id = ? ");
			args.add(q.clientId());
		}
		if (StringUtils.hasText(q.userId())) {
			sql.append(" AND user_id = ? ");
			args.add(q.userId());
		}
		if (StringUtils.hasText(q.username())) {
			sql.append(" AND username = ? ");
			args.add(q.username());
		}
		if (StringUtils.hasText(q.resourceType())) {
			sql.append(" AND resource_type = ? ");
			args.add(q.resourceType());
		}
		if (StringUtils.hasText(q.resourceId())) {
			sql.append(" AND resource_id = ? ");
			args.add(q.resourceId());
		}
		if (q.statusCode() != null) {
			sql.append(" AND status_code = ? ");
			args.add(q.statusCode());
		}

		// ✅ NEW: date range filters (event_time)
		Timestamp fromTs = parseIsoToTs(q.from(), false);
		if (fromTs != null) {
			sql.append(" AND event_time >= ? ");
			args.add(fromTs);
		}

		// choose ONE of these depending on your behavior:
		Timestamp toTs = parseIsoToTs(q.to(), true); // strict timestamp
		// Timestamp toTs = parseIsoToTsEndOfDayIfDateOnly(q.to()); // date-only means end-of-day

		if (toTs != null) {
			sql.append(" AND event_time <= ? ");
			args.add(toTs);
		}
	}

	private static int sanitizePageSize(Integer pageSize) {
		if (pageSize == null || pageSize <= 0) return DEFAULT_PAGE_SIZE;
		return pageSize;
	}

	private static String safe(String s, int max) {
		if (!StringUtils.hasText(s)) return null;
		return s.length() <= max ? s : s.substring(0, max);
	}

	private static String buildNextLink(String baseUrl, AuditQuery q, int pageSize, String nextToken) {
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("?pageSize=").append(pageSize);
		sb.append("&pageToken=").append(urlEncode(nextToken));

		if (StringUtils.hasText(q.tenant())) sb.append("&tenant=").append(urlEncode(q.tenant()));
		if (StringUtils.hasText(q.clientId())) sb.append("&clientId=").append(urlEncode(q.clientId()));
		if (StringUtils.hasText(q.userId())) sb.append("&userId=").append(urlEncode(q.userId()));
		if (StringUtils.hasText(q.username())) sb.append("&username=").append(urlEncode(q.username()));
		if (StringUtils.hasText(q.resourceType())) sb.append("&resourceType=").append(urlEncode(q.resourceType()));
		if (StringUtils.hasText(q.resourceId())) sb.append("&resourceId=").append(urlEncode(q.resourceId()));
		if (null != q.statusCode()) sb.append("&statusCode=").append(q.statusCode());
		if (StringUtils.hasText(q.from())) sb.append("&from=").append(urlEncode(q.from()));
		if (StringUtils.hasText(q.to())) sb.append("&to=").append(urlEncode(q.to()));

		return sb.toString();
	}

	private static String urlEncode(String s) {
		try {
			return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			return s;
		}
	}

	private static final RowMapper<AuditRow> ROW_MAPPER = (ResultSet rs, int rowNum) -> new AuditRow(
		rs.getString("id"),
		rs.getObject("event_time", OffsetDateTime.class),
		rs.getString("http_method"),
		rs.getString("request_path"),
		rs.getString("operation"),
		rs.getString("resource_type"),
		rs.getString("resource_id"),
		rs.getString("tenant"),
		rs.getString("required_scope"),
		rs.getString("roles"),
		rs.getString("scopes"),
		rs.getString("client_id"),
		rs.getString("username"),
		rs.getString("user_id"),
		rs.getString("source_ip"),
		rs.getString("user_agent"),
		rs.getInt("status_code"),
		rs.getInt("duration_ms"),
		rs.getString("error_class"),
		rs.getString("error_message")
	);

	private static final class Cursor {
		final OffsetDateTime eventTime;
		final String id;

		private Cursor(OffsetDateTime eventTime, String id) {
			this.eventTime = eventTime;
			this.id = id;
		}

		static Cursor of(AuditRow row) {
			return new Cursor(row.eventTime(), row.id());
		}

		// token: "<epochMillis>|<uuid>"
		String encode() {
			return eventTime.toInstant().toEpochMilli() + "|" + id;
		}

		static Cursor parse(String token) {
			String[] parts = token.split("\\|", 2);
			long millis = Long.parseLong(parts[0]);
			String id = parts[1];

			OffsetDateTime ts = OffsetDateTime.ofInstant(
				java.time.Instant.ofEpochMilli(millis),
				java.time.ZoneOffset.UTC
			);
			return new Cursor(ts, id);
		}
	}

	private static Timestamp parseIsoToTsEndOfDayIfDateOnly(String iso) {
		if (!StringUtils.hasText(iso)) return null;
		try {
			if (iso.length() == 10) {
				OffsetDateTime odt = OffsetDateTime.of(
					java.time.LocalDate.parse(iso),
					java.time.LocalTime.MAX,
					java.time.ZoneOffset.UTC
				);
				return Timestamp.from(odt.toInstant());
			}
			return Timestamp.from(OffsetDateTime.parse(iso).toInstant());
		} catch (Exception e) {
			return null;
		}
	}

	private static Timestamp parseIsoToTs(String iso, boolean endOfDayIfDateOnly) {
		if (!StringUtils.hasText(iso)) return null;

		try {
			// date-only: yyyy-MM-dd
			if (iso.length() == 10) {
				OffsetDateTime odt = OffsetDateTime.of(
					java.time.LocalDate.parse(iso),
					endOfDayIfDateOnly ? java.time.LocalTime.MAX : java.time.LocalTime.MIN,
					java.time.ZoneOffset.UTC
				);
				return Timestamp.from(odt.toInstant());
			}

			// full ISO-8601 datetime
			return Timestamp.from(OffsetDateTime.parse(iso).toInstant());

		} catch (Exception e) {
			return null; // API layer already validates strictly
		}
	}

}
