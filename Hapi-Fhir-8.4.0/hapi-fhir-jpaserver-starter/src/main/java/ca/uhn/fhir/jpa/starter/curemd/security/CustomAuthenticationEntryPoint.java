package ca.uhn.fhir.jpa.starter.curemd.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Logger;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final Logger LGR = Logger.getLogger(CustomAuthenticationEntryPoint.class.getName());

	@Override
	public void commence(HttpServletRequest request,
								HttpServletResponse response,
								AuthenticationException authException) throws IOException {

		LGR.warning("Authentication failed: " + authException.getMessage());
		prepareResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_TOKEN_MESSAGE);
	}


	private void prepareResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
		response.setStatus(statusCode);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		String jsonResponse = String.format("{\"error\": \"%s\"}", message);
		response.getWriter().write(jsonResponse);
		LGR.warning(message);
	}
}
