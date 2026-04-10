package com.keycloak.security.extensions.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class responsible for sending OTPs via email and SMS in an asynchronous manner
 * using a configurable thread pool. Integrates with Novu for email and a custom SMS gateway.
 *
 * @author Yasir Rehman
 */
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Submits an asynchronous task to send a "new device" notification email via Novu.
     *
     * @param context   current Keycloak authentication flow context
     * @param deviceInfo  device information extracted from request headers
     */
    public static void sendNewDeviceEmail(AuthenticationFlowContext context, DeviceInfo deviceInfo) throws IOException {
        if (StringUtil.isBlank(deviceInfo.getOs())) {
            log.warn("sendNewDeviceEmail: deviceId is blank; skipping email.");
            return;
        }

        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost request = prepareNewDeviceEmailHttpRequest(context, deviceInfo.getOs(), deviceInfo.getIpAddress());
        executePostRequest(httpClient, request, "New Device Email");
    }

    /**
     * Executes the HTTP request and logs the response details.
     *
     * @param httpClient the HTTP client
     * @param request    the HTTP request to send
     * @param label      label for logging context (e.g., "SMS", "Novu")
     * @throws IOException if the request fails
     */
    private static void executePostRequest(CloseableHttpClient httpClient, HttpPost request, String label){
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.infof("%s API response code: %d", label, statusCode);
            log.debugf("%s API response body: %s", label, responseBody);

            if (statusCode < 200 || statusCode >= 300) {
                log.warn("Failed to trigger " + label + " API: " + statusCode + " - " + responseBody);
            }
        } catch (Exception e) {
            log.errorf(e, "Error calling %s API: %s", label, e.getMessage());
        }
    }

    /**
     * Checks whether any of the provided strings are null or empty.
     *
     * @param values strings to check
     * @return true if any value is null or empty, false otherwise
     */
    private static boolean isAnyNullOrEmpty(String... values) {
        for (String val : values) {
            if (StringUtil.isNullOrEmpty(val)) return true;
        }
        return false;
    }

    /**
     * Builds an {@link HttpPost} request to trigger a Novu workflow for "new device" email.
     *
     * @param context   current Keycloak authentication flow context
     * @param deviceId  device identifier to include in the email
     * @param ipAddress caller IP address to include in the email
     * @return an {@link HttpPost} ready to execute; {@code null} if configuration or inputs are invalid
     */
    public static HttpPost prepareNewDeviceEmailHttpRequest(AuthenticationFlowContext context, String deviceId, String ipAddress) throws IOException  {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();

        if (config == null || config.getConfig() == null) {
            log.errorf("Authenticator config is not properly set.");
            return null;
        }

        String uri = config.getConfig().get(Constant.NOVU_URI);
        String apiKey = config.getConfig().get(Constant.NOVU_API_KEY);
        String workflowName = config.getConfig().get(Constant.WORK_FLOW_NAME);

        if (isAnyNullOrEmpty(uri, apiKey, workflowName)) {
            log.errorf("Novu configuration values are missing (URI, API Key, or Workflow Name).");
            return null;
        }
        Map<String, Object> body = prepareNewDeviceEmailBody(user, workflowName, deviceId, ipAddress);
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpPost request = new HttpPost(uri);
        request.setHeader("Authorization", "ApiKey " + apiKey);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody));

        log.debugf("New-device email POST body: %s", jsonBody);
        return request;
    }

    /**
     * Constructs the Novu trigger payload for a "new device" email.
     *
     * @param user         the target Keycloak user
     * @param workflowName Novu workflow name to trigger
     * @param deviceId     device identifier
     * @param ipAddress    optional IP address
     * @return a map representing the JSON body for Novu's trigger API
     */
    private static Map<String, Object> prepareNewDeviceEmailBody(UserModel user, String workflowName, String deviceId, String ipAddress) {
        Map<String, Object> to = Map.of("subscriberId", user.getUsername());
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", user.getFirstName());
        payload.put("email", user.getEmail());
        payload.put("deviceId", deviceId);
        payload.put("ipAddress", ipAddress);

        return Map.of(
                "name", workflowName,
                "to", to,
                "payload", payload
        );
    }

}
