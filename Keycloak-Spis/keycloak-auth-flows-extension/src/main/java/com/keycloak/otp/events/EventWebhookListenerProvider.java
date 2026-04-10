package com.keycloak.otp.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.EventPayload;
import com.keycloak.otp.util.FailedEventLogRequest;
import com.keycloak.otp.util.Utils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class EventWebhookListenerProvider implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(EventWebhookListenerProvider.class);

    private final KeycloakSession keycloakSession; // shared
    private final CloseableHttpClient httpClient; // shared
    private final ExecutorService executor;       // shared
    private final String webhookUrl;
    private final ObjectWriter objectWriter;

    public EventWebhookListenerProvider(KeycloakSession session, CloseableHttpClient httpClient,
                                        ExecutorService executor,
                                        String webhookUrl,
                                        ObjectMapper mapper) {
        this.keycloakSession = session;
        this.httpClient = httpClient;
        this.executor = executor;
        this.webhookUrl = webhookUrl;
        // Create an ObjectWriter once (thread-safe)
        this.objectWriter = mapper.writer();
    }

    @Override
    public void onEvent(Event event) {
        try {
            logger.infof("Received event: type=%s realm=%s user=%s client=%s session=%s ip=%s details=%s error=%s",
                    event.getType(), event.getRealmId(), event.getUserId(), event.getClientId(),
                    event.getSessionId(), event.getIpAddress(), event.getDetails(), event.getError());


            if (!isFailedLoginOrRegisterEvent(event)) {
                logger.debugf("Skipping event: not an authorization_code login: %s", event.getDetails());
                return;
            }

            // --- Serialize to bytes (efficient) and send async ---
//            final byte[] bodyBytes = objectWriter.writeValueAsBytes(buildPayload(event));
//
//            // Submit HTTP send to executor. DO NOT use KeycloakSession here.

            LeapIntegrationService service = new LeapIntegrationService(keycloakSession);
            service.send(buildFailedEventPayload(event));
//            executor.submit(() -> {
//                service.send(buildFailedEventPayload(event));
//            });


        } catch (Exception e) {
            logger.error("Failed to prepare session log payload", e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // ignore or implement similar mapping if needed
    }

    @Override
    public void close() {
        // nothing to close here (httpClient lifecycle managed by factory/Keycloak)
    }

    private FailedEventLogRequest buildFailedEventPayload(Event event) {
        if (event == null || event.getError() == null) {
            return null;
        }

        final String eventType = event.getType() != null ? event.getType().toString() : "UNKNOWN";
        final String realmId = event.getRealmId();
        final String userId = event.getUserId();
        final String clientId = event.getClientId();
        final String sessionId = event.getSessionId();
        final String ip = event.getIpAddress();
        final Map<String, String> details = event.getDetails();


        final String deviceOs = details != null ? details.getOrDefault("deviceOs", "") : "";
        final String deviceId = details != null ? details.getOrDefault("deviceId", "") : "";
        final String appVersion = details != null ? details.getOrDefault("appVersion", "") : "";
        final String xForwardedFor = details != null ? details.getOrDefault("X-Forwarded-For", "") : "";
        final String clientIp = details != null ? details.getOrDefault(Constants.CLIENT_IP, event.getIpAddress()) : event.getIpAddress();

        final String method = details != null ? details.get(Constants.OTP_METHOD) : "";
        final int attemptNumber = details != null ? Utils.convertToInt(details.get(Constants.ATTEMPT_NUMBER), 0) : 0;
        final String codeDeliveredTo = details != null ? details.get(Constants.CODE_DELIVER_TO) : "";
        final String codeIssuedAt = details != null ? details.get(Constants.CODE_ISSUED_AT) : "";
        final int codeExpirySeconds = details != null ? Utils.convertToInt(details.get(Constants.CODE_EXPIRY_SECONDS), 0) : 0;


        final String userAgent = extractUserAgent();
        ResolvedUserInfo userInfo = resolveUserInfo(keycloakSession, event);
        final String username = userInfo.username != null ? userInfo.username : userId;

        FailedEventLogRequest req = new FailedEventLogRequest();
        req.authFlowId = eventType;

        req.attemptedPhone = resolvePhoneNumber(userInfo);
        req.attemptedEmail = userInfo.email;
        req.resolvedUserEmail = userInfo.email;
        req.resolvedUserId = userInfo.userId;
        req.resolvedUserName = username;
        req.resolvedUserType = "Patient";
        req.attemptResult = resolveAttemptResult(event);

        req.systemErrorCode = event.getError();
        req.failureDetail = resolveFailureDetails(event);

        req.sessionId = event.getSessionId();
        req.clientId = event.getClientId();
        req.clientIP = clientIp;
        req.clientPort = event.getIpAddress();
        req.userAgent = extractUserAgent();

        req.appType = "WebApp";
        req.appVersion = "1.1";
//        req.deviceType = "26.3.3";
//        req.deviceName = "26.3.3";
//        req.deviceOS = "26.3.3";

        if (StringUtil.isNotBlank(codeIssuedAt)) {
            FailedEventLogRequest.TwoFactorInfo tf = new FailedEventLogRequest.TwoFactorInfo();
            tf.method = method;
            tf.attemptNumber = attemptNumber;
            tf.codeDeliveredTo = codeDeliveredTo;
            tf.codeIssuedAt = codeIssuedAt;
            tf.codeExpirySeconds = codeExpirySeconds;
            req.twoFactor = tf;
        }

        logger.info("Built failed event log request: " + req);
        return req;
    }

    private String extractUserAgent() {
        if (keycloakSession == null) {
            return "UNKNOWN";
        }

        try {
            if (keycloakSession.getContext() != null &&
                    keycloakSession.getContext().getHttpRequest() != null &&
                    keycloakSession.getContext().getHttpRequest().getHttpHeaders() != null) {

                String ua = keycloakSession.getContext()
                        .getHttpRequest()
                        .getHttpHeaders()
                        .getHeaderString("User-Agent");
                logger.infof("Extracted User-Agent header: %s", ua);
                return ua != null ? ua : "UNKNOWN";
            }
        } catch (Exception ignored) {
        }

        return "UNKNOWN";
    }

    public record ResolvedUserInfo(
            String userId,
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
    }

    private ResolvedUserInfo resolveUserInfo(KeycloakSession session, Event event) {

        if (event == null) {
            return new ResolvedUserInfo(null, null, null, null, null, null);
        }

        String userId = event.getUserId();
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        String phoneNumber = null;


        if (userId != null && session != null && event.getRealmId() != null) {
            RealmModel realm = session.realms().getRealm(event.getRealmId());

            if (realm != null) {
                UserModel user = session.users().getUserById(realm, userId);

                if (user != null) {
                    username = user.getUsername();
                    email = user.getEmail();
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phoneNumber = user.getFirstAttribute(Constants.MOBILE_NUMBER);
                } else {
                    username = event.getDetails() != null ? event.getDetails().get(Constants.USERNAME_OR_EMAIL) : null;
                }
            }
        }

        return new ResolvedUserInfo(
                userId,
                safe(username),
                safe(email),
                safe(firstName),
                safe(lastName),
                safe(phoneNumber)
        );
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private boolean isFailedLoginOrRegisterEvent(Event event) {
        if (event == null) return false;

        Map<String, String> details = event.getDetails();
        if (StringUtil.isNotBlank(event.getError()) && details != null) {
            // 1) Token endpoint case (explicit grant_type)
            String grantType = details.get("grant_type");
            if ("authorization_code".equalsIgnoreCase(grantType)) {
                return true;
            }

            // 2) Browser auth case — auth endpoint parameters are sometimes present in details
            return details.containsKey("redirect_uri") ||
                    details.containsKey("response_type") ||
                    details.containsKey("code_id") ||
                    details.containsKey("auth_method");
        }

        return false;
    }

    private String resolveFailureDetails(Event event) {
        if (event == null) {
            return "Failure occurred";
        }

        String failureDetail = null;
        if (event.getDetails() != null) {
            failureDetail = event.getDetails().get(Constants.FAILURE_DETAILS);
        }

        if (StringUtil.isNotBlank(failureDetail)) {
            return switch (failureDetail) {

                // Login
                case Constants.INVALID_USERNAME -> "Login failed because the username was invalid.";
                case Constants.INVALID_PASSWORD -> "Login failed because the password was invalid.";
                case Constants.USER_DISABLED -> "Login failed because the user account is disabled.";

                // OTP
                case Constants.OTP_RESEND_MAX_REACHED -> "OTP resend limit has been reached.";
                case Constants.OTP_INVALID -> "The OTP code entered is invalid.";
                case Constants.OTP_EXPIRED -> "The OTP code has expired.";
                case Constants.OTP_BRUTE_FORCE_WAIT ->
                        "Too many attempts were made. OTP verification is temporarily blocked.";
                case Constants.OTP_TOO_MANY_ATTEMPTS -> "Maximum invalid OTP attempts reached.";

                // Registration
                case Constants.INVALID_REGISTRATION_DATA -> "Registration failed due to invalid or missing data.";
                case Constants.EMAIL_ALREADY_EXISTS -> "Registration failed because the email already exists.";
                case Constants.USERNAME_ALREADY_EXISTS -> "Registration failed because the username already exists.";
                case Constants.LEAP_REGISTRATION_FAILED -> "Registration failed while creating the user in Leap.";
                case Constants.INTERNAL_ERROR -> "Registration failed due to an internal error.";


//                // Reset Password
                case Constants.MISSING_USERNAME -> "The username was missing.";
                case Constants.USER_NOT_FOUND -> "No user was found for the provided username.";
                case Constants.INVALID_PASSWORD_EXISTING ->
                        "Password reset failed because the current password is invalid.";
                case Constants.PASSWORD_POLICY_VALIDATION_FAILED ->
                        "Password reset failed because the new password did not meet policy requirements.";

                default -> failureDetail;
            };
        }

        String error = event.getError();
        if (StringUtil.isNullOrEmpty(error)) {
            return "Failure occurred";
        }

        return switch (error) {
            case Errors.INVALID_USER_CREDENTIALS -> "Authentication failed due to invalid user credentials.";
            case Errors.USER_NOT_FOUND -> "No user was found for the provided identifier.";
            case Errors.USER_DISABLED -> "The user account is disabled.";
            case Errors.USER_TEMPORARILY_DISABLED ->
                    "The user account is temporarily disabled due to too many failed attempts.";
            case "Errors.USER_SUSPENDED" -> "The user account is suspended.";
            case Errors.EXPIRED_CODE -> "The OTP code has expired.";
            case Errors.INVALID_CODE -> "The OTP code is invalid.";
            case "too_many_requests" -> "Too many requests were received. Please try again later.";
            case Errors.INVALID_REGISTRATION -> "Registration failed due to invalid or duplicate data.";
            case "update_password_error" -> "Password update failed.";
            default -> "Authentication failed due to: " + error;
        };
    }

    private static String resolveAttemptResult(Event event) {

        if (event == null) {
            return Constants.SYSTEM_ERROR;
        }

        String error = event.getError();

        if (error == null) {
            return Constants.SYSTEM_ERROR;
        }

        return switch (error) {
            case "invalid_user_credentials" -> Constants.INVALID_CREDENTIALS;
            case "user_not_found" -> Constants.ACCOUNT_NOT_FOUND;
            case "user_disabled" -> Constants.ACCOUNT_INACTIVE;
            case "user_temporarily_disabled" -> Constants.ACCOUNT_LOCKED;
            case "user_suspended" -> Constants.ACCOUNT_SUSPENDED;
            case "expired_code" -> Constants.TWO_FACTOR_EXPIRED;
            case "invalid_code" -> Constants.TWO_FACTOR_FAILED;
            case "too_many_requests" -> Constants.RATE_LIMITED;
            default -> Constants.SYSTEM_ERROR;
        };
    }

    private String resolvePhoneNumber(ResolvedUserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }
        String phone = userInfo.phoneNumber;
        if (StringUtil.isNotBlank(phone)) {
            return phone;
        }
        if (!Utils.isEmail(userInfo.username)) {
            return userInfo.username;
        }
        return "";
    }

    private void sendHttp(byte[] bodyBytes) {
        HttpPost post = new HttpPost(webhookUrl);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        // add auth header if needed, e.g. post.setHeader("Authorization", "Bearer ...");
        post.setEntity(new ByteArrayEntity(bodyBytes, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse resp = httpClient.execute(post)) {
            int status = resp.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                logger.infof("Posted session log, status=%d, respBody=%s", status, respBody);
            } else {
                String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                logger.warnf("Non-2xx response when posting session log: status=%d response=%s", status, respBody);
            }
        } catch (Exception e) {
            // No retry as requested; just log
            logger.errorf(e, "Failed to post session log (no retry). payload_size=%d", bodyBytes.length);
        }
    }

    private EventPayload buildPayload(Event event) {
        EventPayload payload = new EventPayload();

        // --- EXTRACT synchronously from Event & store into local vars ---
        final String eventType = event.getType() != null ? event.getType().toString() : "UNKNOWN";
        final String realmId = event.getRealmId();
        final String userId = event.getUserId();
        final String clientId = event.getClientId();
        final String sessionId = event.getSessionId();
        final String ip = event.getIpAddress();
        final Map<String, String> details = event.getDetails();

        final String deviceOs = details != null ? details.getOrDefault("deviceOs", "") : "";
        final String deviceId = details != null ? details.getOrDefault("deviceId", "") : "";
        final String appVersion = details != null ? details.getOrDefault("appVersion", "") : "";
        final String xForwardedFor = details != null ? details.getOrDefault("X-Forwarded-For", "") : "";

        final String userAgent = extractUserAgent();
        ResolvedUserInfo userInfo = resolveUserInfo(keycloakSession, event);
        final String username = userInfo.username != null ? userInfo.username : userId;

        if (event.getError() != null) {
            payload.errorCode = event.getError();
            payload.failureReason = (details != null) ? details.getOrDefault("reason", "") : "";
        }

        // metadata
        payload.realmId = realmId != null ? realmId : "";
        payload.clientId = clientId != null ? clientId : "";
        payload.eventType = eventType;

        // user info
        payload.userId = userId != null ? userId : "";
        payload.userName = username;
        payload.email = StringUtil.isNotBlank(userInfo.email) ? userInfo.email : username;
        payload.name = Utils.buildFullName(userInfo.firstName(), userInfo.lastName(), username);
        payload.userType = details != null ? details.getOrDefault("userType", "Patient") : "Patient";
        payload.practiceId = details != null ? details.getOrDefault("PracticeId", "") : "";
        payload.type = details != null ? details.getOrDefault("type", "Patient") : "Patient";

        // session info
        String sessionStart = details != null ? details.getOrDefault("sessionStart", null) : null;
        if (sessionStart == null) {
            sessionStart = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
        }
        payload.id = details != null ? details.getOrDefault("id", sessionId != null ? sessionId : "") : (sessionId != null ? sessionId : "");
        payload.sessionId = sessionId != null ? sessionId : "";
        payload.status = eventType;
        payload.sessionStart = sessionStart;
        payload.lastActive = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(event.getTime()));
        payload.timeZone = details != null ? details.getOrDefault("timeZone", "UTC") : "UTC";

        // device info
//        payload.deviceId = deviceId;
//        payload.deviceOs = deviceOs;
        payload.deviceName = details != null ? details.getOrDefault("deviceName", "") : "";
        payload.deviceType = details != null ? details.getOrDefault("deviceType", "Web") : "Web";
//        payload.device = deviceOs;
        payload.appType = details != null ? details.getOrDefault("appType", "Web") : "Web";
        payload.appVersion = appVersion;

        // network info
        String clientIp = (xForwardedFor != null && !xForwardedFor.isEmpty()) ? xForwardedFor.split(",")[0].trim() : (ip != null ? ip : "");
        payload.clientIp = clientIp;
        payload.serverIp = details != null ? details.getOrDefault("serverIp", "127.0.0.1") : "127.0.0.1";
        payload.serverPort = details != null ? details.getOrDefault("serverPort", "8080") : "8080";
        payload.userAgent = userAgent;

        // geo info (if available)
        payload.geoCity = details != null ? details.getOrDefault("GeoCity", "") : "";
        payload.geoCountry = details != null ? details.getOrDefault("GeoCountry", "") : "";

        logger.info("Built event payload: " + payload);
        return payload;
    }
}
