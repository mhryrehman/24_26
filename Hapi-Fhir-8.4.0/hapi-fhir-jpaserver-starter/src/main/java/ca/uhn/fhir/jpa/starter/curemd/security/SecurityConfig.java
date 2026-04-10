package ca.uhn.fhir.jpa.starter.curemd.security;

import ca.uhn.fhir.jpa.starter.curemd.KeycloakConstants;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.JsonSerialization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Author: Yasir Rehman
 * Description:
 * This class configures the security settings for the application using Spring Security.
 * It defines a security filter chain that handles authentication and authorization,
 * integrates Keycloak's policy enforcement, and sets up CORS (Cross-Origin Resource Sharing) policies.
 */
@Configuration
public class SecurityConfig {

	private static final List<String> ALLOWED_ORIGINS = List.of("*");
	private static final List<String> ALLOWED_HEADERS = List.of("origin", "content-type", "accept", "x-requested-with", "Authorization");
	private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH");
	private static final long CORS_MAX_AGE = 3600L;
	private static final RequestMatcher smartConfigMatcher = new RegexRequestMatcher(Constants.SMART_CONFIG_URL_REGEX, GET.name());
	private static final RequestMatcher metadataMatcher = new RegexRequestMatcher(Constants.METADATA_URL_REGEX, GET.name());

	@Autowired
	private CustomAuthorizationFilter customAuthorizationFilter;

	/**
	 * Configures the security filter chain for the application.
	 * Allows certain endpoints to be publicly accessible and secures the rest with JWT-based authentication.
	 *
	 * @param http the HttpSecurity object for configuring security
	 * @return the configured SecurityFilterChain
	 * @throws Exception if there is a problem configuring the SecurityFilterChain
	 */
	@Bean
	public SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {

		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(smartConfigMatcher).permitAll()
				.requestMatchers(metadataMatcher).permitAll()
				.requestMatchers(GET, Constants.PUBLIC_URIS.toArray(new String[0])).permitAll()
				.requestMatchers("/", "/home").permitAll()
				.requestMatchers("/js/**", "/css/**", "/images/**", "/html/**").permitAll()
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/fhir/**").authenticated());

		http.oauth2ResourceServer(oauth2 -> oauth2
			.opaqueToken(withDefaults())
			.authenticationEntryPoint(new CustomAuthenticationEntryPoint()));

		return http.build();
	}

	/**
	 * Configures CORS settings for the application.
	 * Allows specific origins, headers, and methods for cross-origin requests.
	 *
	 * @return the configured CorsConfigurationSource
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(ALLOWED_ORIGINS);
		configuration.setAllowedMethods(ALLOWED_METHODS);
		configuration.setAllowedHeaders(ALLOWED_HEADERS);
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(CORS_MAX_AGE);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * Creates a custom Keycloak policy enforcer filter to integrate Keycloak authorization policies
	 * using a `policy-enforcer.json` configuration file.
	 *
	 * @return an instance of `CustomServletPolicyEnforcerFilter`
	 */
	private CustomServletPolicyEnforcerFilter createPolicyEnforcerFilter() {
		return new CustomServletPolicyEnforcerFilter(new ConfigurationResolver() {

			@Override
			public PolicyEnforcerConfig resolve(HttpRequest httpRequest) {
				try {
					return JsonSerialization.readValue(getClass().getResourceAsStream("/policy-enforcer.json"),
						PolicyEnforcerConfig.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
