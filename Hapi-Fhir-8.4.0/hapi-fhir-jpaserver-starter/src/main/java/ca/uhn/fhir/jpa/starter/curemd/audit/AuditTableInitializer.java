package ca.uhn.fhir.jpa.starter.curemd.audit;

import org.jboss.logging.Logger;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AuditTableInitializer {
	private static final Logger LOG = Logger.getLogger(AuditTableInitializer.class);

	@Bean
	public ApplicationRunner createAuditTableIfMissing(JdbcTemplate jdbcTemplate) {
		LOG.info("Initializing fhir_audit_log table if missing");
		return args -> {
			jdbcTemplate.execute("""
					CREATE TABLE IF NOT EXISTS fhir_audit_log (
						id uuid PRIMARY KEY,
						event_time timestamptz NOT NULL,

						http_method varchar(10) NOT NULL,
						request_path varchar(2048) NOT NULL,
						query_string varchar(4096),

						operation varchar(50) NOT NULL,
						resource_type varchar(100),
						resource_id varchar(200),

						tenant varchar(200),
						required_scope varchar(200),
						roles varchar(4000),
						scopes varchar(4000),
						client_id varchar(256),
						username varchar(256),
						user_id varchar(256),

						source_ip varchar(64),
						user_agent varchar(512),

						status_code integer NOT NULL,
						duration_ms integer NOT NULL,

						error_class varchar(256),
						error_message varchar(2000)
					)
				""");

			// indexes for reporting
			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_time
					ON fhir_audit_log (event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_tenant_time
					ON fhir_audit_log (tenant, event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_client_time
					ON fhir_audit_log (client_id, event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_user_time
					ON fhir_audit_log (user_id, event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_res_time
					ON fhir_audit_log (resource_type, resource_id, event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_fhir_audit_status_time
					ON fhir_audit_log (status_code, event_time)
				""");

			jdbcTemplate.execute("""
					CREATE INDEX IF NOT EXISTS idx_audit_tenant_time_id
					ON fhir_audit_log (tenant, event_time DESC, id DESC)
				""");
		};
	}
}
