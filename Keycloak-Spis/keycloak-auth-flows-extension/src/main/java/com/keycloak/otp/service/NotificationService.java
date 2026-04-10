package com.keycloak.otp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keycloak.otp.model.UserDto;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class responsible for sending OTPs via email and SMS in an asynchronous manner
 * using a configurable thread pool. Integrates with Novu for email and a custom SMS gateway.
 *
 * @author Yasir Rehman
 */
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExecutorService executor;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    static {
        executor = ThreadPoolFactory.buildExecutorFromEnv();
    }

    /**
     * Submits asynchronous tasks to send OTP via email Provider and SMS.
     *
     * @param context the current Keycloak authentication flow context
     * @param otp     the one-time password to be sent
     * @param expiry  the expiry time of the OTP in seconds
     * @throws IOException if preparing the payload fails
     */
    public static void sendOtpViaEmailProviderAndSms(AuthenticationFlowContext context, String otp, int expiry) throws Exception {
        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        UserModel user = context.getUser();

        String otpEmailEnabled = user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED);
        String otpSmsEnabled = user.getFirstAttribute(Constants.OTP_SMS_ENABLED);
        boolean isAlwaysExecute = Utils.getConfigBool(context.getAuthenticatorConfig(), Constants.ALWAYS_EXECUTE, false);

        log.infof("kcId=%s OTP Email Enabled: %s, OTP SMS Enabled: %s, Always Execute: %b", user.getId(),
                otpEmailEnabled, otpSmsEnabled, isAlwaysExecute);

        if (isAlwaysExecute) {
            String identifier = context.getAuthenticationSession().getAuthNote(Constants.NOTE_EMAIL);
            log.infof("kcId=%s Determining OTP channel based on identifier: %s", user.getId(), identifier);
            boolean looksLikeEmail = Utils.isEmail(identifier);
            // If identifier looks like an email, enable email OTP and disable SMS OTP, and vice versa
            otpEmailEnabled = looksLikeEmail ? "true" : "false";
            otpSmsEnabled = looksLikeEmail ? "false" : "true";
        }

        UserDto userDto = new UserDto(
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                getUserAttribute(user, Constants.MOBILE_NUMBER)
        );

        if (Boolean.parseBoolean(otpEmailEnabled)) {
            sendOtpViaEmailProvider(context, user, otp, expiry);
        }

        if (Boolean.parseBoolean(otpSmsEnabled)) {
            sendAsync(httpClient, prepareSmsOtpHttpRequest(context, userDto, otp), "SMS", user.getId());
        }
    }

    /**
     * Submits asynchronous tasks to send OTP via email and SMS.
     *
     * @param context the current Keycloak authentication flow context
     * @param otp     the one-time password to be sent
     * @param expiry  the expiry time of the OTP in seconds
     * @throws IOException if preparing the payload fails
     */
    public static void sendOtpAsync(AuthenticationFlowContext context, String otp, int expiry) throws IOException {
        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        UserModel user = context.getUser();

        String otpEmailEnabled = user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED);
        String otpSmsEnabled = user.getFirstAttribute(Constants.OTP_SMS_ENABLED);
        boolean isAlwaysExecute = Utils.getConfigBool(context.getAuthenticatorConfig(), Constants.ALWAYS_EXECUTE, false);

        log.infof("kcId=%s OTP Email Enabled: %s, OTP SMS Enabled: %s, Always Execute: %b", user.getId(),
                otpEmailEnabled, otpSmsEnabled, isAlwaysExecute);

        if (isAlwaysExecute) {
            String identifier = context.getAuthenticationSession().getAuthNote(Constants.NOTE_EMAIL);
            log.infof("kcId=%s Determining OTP channel based on identifier: %s", user.getId(), identifier);
            boolean looksLikeEmail = Utils.isEmail(identifier);
            // If identifier looks like an email, enable email OTP and disable SMS OTP, and vice versa
            otpEmailEnabled = looksLikeEmail ? "true" : "false";
            otpSmsEnabled = looksLikeEmail ? "false" : "true";
        }

        UserDto userDto = new UserDto(
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                getUserAttribute(user, Constants.MOBILE_NUMBER)
        );

        if (Boolean.parseBoolean(otpEmailEnabled)) {
            sendAsync(httpClient, prepareEmailOtpHttpRequest(context, userDto, otp, expiry), "Email", user.getId());
        }

        if (Boolean.parseBoolean(otpSmsEnabled)) {
            sendAsync(httpClient, prepareSmsOtpHttpRequest(context, userDto, otp), "SMS", user.getId());
        }
    }

    public static void sendOtpAsyncEmail(AuthenticationFlowContext context, UserDto userDto, String otp, int expiry) throws IOException {
        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        log.infof("kcId=%s Sending OTP Email only.", userDto.getUsername());

        sendAsync(httpClient, prepareEmailOtpHttpRequest(context, userDto, otp, expiry), "Email", userDto.getUsername());
    }

    public static void sendOtpAsyncSms(AuthenticationFlowContext context, UserDto userDto, String otp, int expiry) throws IOException {
        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        log.infof("kcId=%s Sending OTP SMS only.", userDto.getUsername());

        sendAsync(httpClient, prepareSmsOtpHttpRequest(context, userDto, otp), "SMS", userDto.getUsername());
    }

    /**
     * Executes an asynchronous HTTP POST request to deliver an OTP message via the specified channel.
     *
     * @param httpClient the {@link CloseableHttpClient} used to send the request
     * @param request    the prepared {@link HttpPost} request for the channel (email or SMS)
     * @param channel    a name for the delivery channel
     */
    private static void sendAsync(CloseableHttpClient httpClient, HttpPost request, String channel, String userId) {
        if (request == null) {
            log.warn(channel + " OTP request is null. Skipping " + channel + " OTP sending.");
            return;
        }

        executor.submit(() -> {
            try {
                log.infof("kcId=%s Sending OTP via " + channel + " by thread: " + Thread.currentThread().getName(), userId);
                executePostRequest(httpClient, request, channel, userId);
            } catch (Exception e) {
                log.error("Failed to send OTP via " + channel, e);
            }
        });
    }

    /**
     * Prepares an HTTP POST request for Novu to send an OTP via email.
     *
     * @param context the Keycloak authentication context
     * @param otp     the one-time password
     * @param expiry  OTP expiration time in seconds
     * @return the HttpPost request ready to be executed
     * @throws IOException if the payload cannot be serialized
     */
    public static HttpPost prepareEmailOtpHttpRequest(AuthenticationFlowContext context, UserDto userDto, String otp, int expiry) throws IOException {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        if (config == null || config.getConfig() == null) {
            log.errorf("Authenticator config is not properly set.");
            return null;
        }

        String uri = config.getConfig().get(Constants.NOVU_URI);
        String apiKey = config.getConfig().get(Constants.NOVU_API_KEY);
        String workflowName = config.getConfig().get(Constants.WORK_FLOW_NAME);

        if (isAnyNullOrEmpty(uri, apiKey, workflowName)) {
            log.errorf("Novu configuration values are missing (URI, API Key, or Workflow Name).");
            return null;
        }

        int expiryInMinutes = expiry / 60;
        Map<String, Object> body = getStringObjectMap(otp, expiryInMinutes, userDto, workflowName);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpPost request = new HttpPost(uri);
        request.setHeader("Authorization", "ApiKey " + apiKey);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody));

        log.infof("kcId=%s Sending POST request with body: %s", userDto.getUsername(), jsonBody);
        return request;
    }

    /**
     * Constructs the payload for Novu API request.
     *
     * @param otp          the one-time password
     * @param expiry       expiry in minutes
     * @param user         the Keycloak user
     * @param workflowName the Novu workflow name
     * @return a map representing the JSON body
     */
    private static Map<String, Object> getStringObjectMap(String otp, int expiry, UserDto user, String workflowName) {
        Map<String, Object> to = Map.of("subscriberId", user.getUsername());
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", user.getFirstName());
        payload.put("email", user.getEmail());
        payload.put("expiry", expiry);
        payload.put("otp", otp);

        for (int i = 0; i < otp.length(); i++) {
            payload.put("otpDigit" + (i + 1), String.valueOf(otp.charAt(i)));
        }

        return Map.of(
                "name", workflowName,
                "to", to,
                "payload", payload
        );
    }

    /**
     * Prepares an HTTP POST request to send an OTP SMS via custom API.
     *
     * @param context the Keycloak authentication context
     * @param otp     the one-time password
     * @return HttpPost request ready for execution
     * @throws IOException if payload serialization fails
     */
    public static HttpPost prepareSmsOtpHttpRequest(AuthenticationFlowContext context, UserDto userDto, String otp) throws IOException {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null || config.getConfig() == null) {
            log.warn("SMS Authenticator config is not set.");
            return null;
        }

        String smsUri = config.getConfig().get(Constants.SMS_URL);
        String messageTemplate = config.getConfig().get(Constants.MESSAGE_TEXT);
        String messageType = config.getConfig().get(Constants.MESSAGE_TYPE);

        if (isAnyNullOrEmpty(smsUri, messageTemplate, messageType)) {
            log.warn("SMS configuration values are missing (URI, message template, or message type).");
            return null;
        }

        if (StringUtil.isNullOrEmpty(userDto.getPhoneNumber())) {
            log.warnf("User [%s] does not have a phone number set.", userDto.getUsername());
            return null;
        }

        String message = messageTemplate
                .replace("{otpModel.FirstName}", Optional.ofNullable(userDto.getFirstName()).orElse(""))
                .replace("{otpModel.OTP}", otp);

        String bearerToken = getTokenForCureMDApi(context);
        if (StringUtil.isNullOrEmpty(bearerToken)) {
            log.warn("Failed to retrieve Bearer token. SMS will not be sent.");
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("ToNumber", userDto.getPhoneNumber());
        payload.put("Message", message);
        payload.put("MessageType", messageType);
        payload.put("RecipientConsent", false);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpPost request = new HttpPost(smsUri);
        request.setHeader("Authorization", "Bearer " + bearerToken);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody));

        log.infof("Sending SMS request with body: %s", jsonBody);
        return request;
    }

    /**
     * Retrieves a bearer token from a configured endpoint used for SMS authorization.
     *
     * @param context the Keycloak authentication context
     * @return bearer token or null if request fails
     * @throws IOException if request setup fails
     */
    public static String getTokenForCureMDApi(AuthenticationFlowContext context) throws IOException {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        if (config == null || config.getConfig() == null) {
            log.warn("Authenticator config is missing or not properly initialized.");
            return null;
        }

        String tokenUrl = config.getConfig().get(Constants.TOKEN_URL);
        String requestBody = config.getConfig().get(Constants.TOKEN_REQUEST_BODY);

        if (isAnyNullOrEmpty(tokenUrl, requestBody)) {
            log.warn("Token configuration missing: token URI or request body is null or empty.");
            return null;
        }

        HttpPost request = new HttpPost(tokenUrl);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setEntity(new StringEntity(requestBody, java.nio.charset.StandardCharsets.UTF_8));

        log.debugf("Sending request with body: %s", requestBody);
        HttpClientProvider clientProvider = context.getSession().getProvider(HttpClientProvider.class);

        try {
            CloseableHttpClient httpClient = clientProvider.getHttpClient();
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.infof("Token API responded with status code: %d", statusCode);
            log.debugf("Token API response body: %s", responseBody);

            if (statusCode == 200) {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                return Optional.ofNullable(jsonNode.get("access_token"))
                        .map(JsonNode::asText)
                        .orElse(null);
            } else {
                log.warnf("Token request failed with status %d: %s", statusCode, responseBody);
            }
        } catch (IOException e) {
            log.error("IOException while calling token API", e);
        } catch (Exception e) {
            log.error("Unexpected error while calling token API", e);
        }

        return null;
    }

    /**
     * Executes the HTTP request and logs the response details.
     *
     * @param httpClient the HTTP client
     * @param request    the HTTP request to send
     * @param label      label for logging context (e.g., "SMS", "Novu")
     * @throws IOException if the request fails
     */
    private static void executePostRequest(CloseableHttpClient httpClient, HttpPost request, String label, String userId) throws IOException {
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.infof("kcId=%s %s API response code: %d", userId, label, statusCode);
            log.debugf("kcId=%s %s API response body: %s", userId, label, responseBody);
            if (statusCode < 200 || statusCode >= 300) {
                log.warn("Failed to trigger " + label + " API: " + statusCode + " - " + responseBody);
            }
        } catch (Exception e) {
            log.errorf(e, "Error calling %s API: %s", label, e.getMessage());
        }
    }

    /**
     * Retrieves a single user attribute by key.
     *
     * @param user the Keycloak user
     * @param key  the attribute key
     * @return the attribute value or null
     */
    private static String getUserAttribute(UserModel user, String key) {
        return Optional.ofNullable(user.getAttributes().get(key))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
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

    private void sendVerificationCode(AuthenticationFlowContext context, String code, String ttl)
            throws EmailException {
        Map<String, Object> bodyAttributes = new HashMap<>();

        bodyAttributes.put("code", code);
        bodyAttributes.put("ttl", ttl);

        context
                .getSession()
                .getProvider(EmailTemplateProvider.class)
                .setAuthenticationSession(context.getAuthenticationSession())
                .setRealm(context.getRealm())
                .setUser(context.getUser())
                .send("email.2fa.mail.subject", "email.ftl", bodyAttributes);
    }

    /**
     * Sends an OTP email using the configured templates
     * and Keycloak EmailSenderProvider.
     *
     * @param context    authentication context
     * @param user       target user
     * @param otp        generated one-time password
     * @param ttlSeconds expiry time in seconds
     * @throws Exception if email sending fails
     */
    public static void sendOtpViaEmailProvider(AuthenticationFlowContext context,
                                               UserModel user, String otp, int ttlSeconds) throws Exception {
        if (StringUtil.isNullOrEmpty(user.getEmail())) {
            throw new IllegalArgumentException("toEmail is required");
        }
        AuthenticatorConfigModel cfg = context.getAuthenticatorConfig();
        RealmModel realm = context.getRealm();
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();

        // config-based templates (editable on authenticator execution)
        String subjectTplConfig = Utils.getConfigString(cfg, "emailSubjectTemplate", "Your verification code");
        String textTplConfig = Utils.getConfigString(cfg, "emailTextBodyTemplate", "Your verification code is ${code}. It expires in ${expiresMinutes} minutes.");
        String htmlTplConfig = Utils.getConfigString(cfg, "emailHtmlBodyTemplate", "<p>Your verification code is <strong>${code}</strong>. It will expire in ${expiresMinutes} minutes.</p>");
        log.infof("Using email templates - Subject: %s, Text: %s, HTML: %s", subjectTplConfig, textTplConfig, htmlTplConfig);

        // 3) attributes for templates
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("otp", otp);
        attrs.put("expiry", ttlSeconds / 60);
        attrs.put("realmName", realm.getDisplayName() == null ? realm.getName() : realm.getDisplayName());
        String loginHint = auth.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (loginHint != null) attrs.put("user", loginHint);
        attrs.put("name", buildDisplayName(user));

        // Render subject/text/html from config templates and call EmailSenderProvider
        String subject = renderTemplate(subjectTplConfig, attrs);
        String textBody = renderTemplate(textTplConfig, attrs);
        String htmlBody = renderTemplate(htmlTplConfig, attrs);

        EmailSenderProvider sender = session.getProvider(EmailSenderProvider.class);
        if (sender == null) {
            throw new IllegalStateException("EmailSenderProvider not available");
        }

        Map<String, String> smtpConfig = realm.getSmtpConfig() == null ? Map.of() : new HashMap<>(realm.getSmtpConfig());

        try {
            log.infof("Sending OTP email to %s with subject: %s", user.getEmail(), subject);
            sender.send(smtpConfig, user.getEmail(), subject, textBody, htmlBody);
        } catch (RuntimeException re) {
            // cleanup on failure
        }
    }

    /**
     * Builds a display name using first and last name.
     * Falls back to username if both are empty.
     *
     * @param user the user model
     * @return display name or empty string
     */
    public static String buildDisplayName(UserModel user) {
        if (user == null) {
            return "";
        }

        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        StringBuilder fullName = new StringBuilder();

        if (!StringUtil.isNullOrEmpty(firstName)) {
            fullName.append(firstName.trim());
        }

        if (!StringUtil.isNullOrEmpty(lastName)) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }

        String result = fullName.toString().trim();

        // If both first and last name are empty → fallback to username
        if (result.isBlank()) {
            String username = user.getUsername();
            if (username != null && !username.isBlank()) {
                return username.trim();
            }
            return "";
        }

        return result;
    }


    /**
     * Replaces ${key} placeholders in the template
     * with values from the provided attributes map.
     *
     * @param template template string
     * @param attrs    attribute map
     * @return rendered string
     */
    private static String renderTemplate(String template, Map<String, Object> attrs) {
        if (template == null) return "";
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = attrs.get(key);
            String replacement = val == null ? "" : escapeHtml(val.toString());
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Escapes basic HTML characters in a string.
     *
     * @param s input string
     * @return escaped string or empty if null
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
