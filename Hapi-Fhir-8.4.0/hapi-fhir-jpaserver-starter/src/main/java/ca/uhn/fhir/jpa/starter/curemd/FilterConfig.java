package ca.uhn.fhir.jpa.starter.curemd;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author: Yasir Rehman
 * Description:
 * Configures a servlet filter to intercept requests to the SMART on FHIR
 * `.well-known/smart-configuration` endpoint. This filter allows for custom
 * processing or modifications to the SMART configuration response.
 */
@Configuration
public class FilterConfig {

	@Bean
	public FilterRegistrationBean<SmartConfigurationFilter> smartConfigFilter() {
		FilterRegistrationBean<SmartConfigurationFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new SmartConfigurationFilter());
		registrationBean.addUrlPatterns("/fhir/.well-known/smart-configuration");
		registrationBean.setOrder(1);
		return registrationBean;
	}
}

