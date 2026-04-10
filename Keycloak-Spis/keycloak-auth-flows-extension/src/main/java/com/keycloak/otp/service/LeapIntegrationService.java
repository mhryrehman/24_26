package com.keycloak.otp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.EventPayload;
import com.keycloak.otp.util.FailedEventLogRequest;
import com.keycloak.otp.util.StateDecoder;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicLong;

public class LeapIntegrationService {

    private static final Logger LOG = Logger.getLogger(LeapIntegrationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final KeycloakSession session;

    // cached token
    private static volatile String cachedToken;
    private static final AtomicLong tokenExpiryEpochSec = new AtomicLong(0);

    public LeapIntegrationService(KeycloakSession session) {
        this.session = session;
    }

    public void logActivity(AuthenticationSessionModel authSession,
                            String action,
                            String workflowName) {
        try {
            LOG.info("Logging user activity: " + action + " in workflow " + workflowName);
            Map<String, String> cfg = resolveLoggingConfig(session.getContext().getRealm());
            if (cfg == null) return;

            String apiUrl = cfg.get(Constants.LOG_API_URL);

            // extract cached sessionId from authNotes or decode once
            String sessionId = authSession.getAuthNote(Constants.LOG_SESSION_ID);
            if (sessionId == null) {
                String rawState = authSession.getClientNote(OIDCLoginProtocol.STATE_PARAM);
                LOG.info("Decoding sessionId from state : " + rawState);
                JsonNode stateJson = StateDecoder.decodeState(rawState);
                sessionId = StateDecoder.getString(stateJson, "sessionId");
                authSession.setAuthNote("log_session_id", sessionId);
            }
            LOG.info("Using sessionId : " + sessionId);

            ObjectNode payload = buildPayload(authSession, action, workflowName, sessionId);
            LOG.info("Payload: " + MAPPER.writeValueAsString(payload));

            // ensure we have a token
            String token = ensureToken(cfg);

            HttpResult r1 = doPostJson(apiUrl, token, payload);

            if (r1.status == 401) {
                LOG.info("Token expired, refreshing…");
                token = refreshToken(cfg);
                HttpResult r2 = doPostJson(apiUrl, token, payload);
                logHttp("[UserActivity retry] ", apiUrl, r2, "retry");
                if (r2.status / 100 != 2) {
                    LOG.warnf("User activity log failed (retry) HTTP %d, body: %s", r2.status, truncate(r2.body));
                }
                return;
            }

            logHttp("[UserActivity] ", apiUrl, r1, "first");

            if (r1.status / 100 != 2) {
                LOG.warnf("User activity log failed (retry) HTTP %d, body: %s", r1.status, truncate(r1.body));
            }

        } catch (Exception e) {
            LOG.error("Error logging activity", e);
        }
    }

    private ObjectNode buildPayload(AuthenticationSessionModel authSession,
                                    String action,
                                    String workflowName,
                                    String sessionId) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("userWorkflow", workflowName);
        p.put("action", action);
        p.put("pageInitializedTime", Instant.now().toString());
        p.put("actionTime", Instant.now().toString());
        p.put("sessionId", sessionId);

        UserModel user = authSession.getAuthenticatedUser();
        String email = authSession.getAuthNote(Constants.NOTE_EMAIL);
        if (user != null) {
            LOG.info("user find in authSession : " + user.getUsername());
            p.put("userId", user.getId());
            p.put("userName", user.getUsername());
            if (user.getEmail() != null) p.put("email", user.getEmail());
        } else if (!StringUtil.isNullOrEmpty(email)) {
            LOG.info("user find in email : " + email);
            p.put("email", email);
            p.put("userName", email);
        }

        String userAgent = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.USER_AGENT);
        if (userAgent != null) p.put("userAgent", userAgent);
        return p;
    }

    private Map<String, String> resolveLoggingConfig(RealmModel realm) {
        AuthenticatorConfigModel cfgModel = realm.getAuthenticatorConfigByAlias("Leap Logging Config");
        if (cfgModel == null) {
            LOG.warn("Leap Logging Config not found in realm");
            return null;
        }
        LOG.info("Loaded logging config : " + cfgModel.getConfig());
        return cfgModel.getConfig();
    }

    private String ensureToken(Map<String, String> cfg) throws Exception {
        long now = Instant.now().getEpochSecond();
        if (cachedToken == null || now >= tokenExpiryEpochSec.get()) {
            LOG.info("Token missing or expired, refreshing…");
            return refreshToken(cfg);
        }
        return cachedToken;
    }

    private String refreshToken(Map<String, String> cfg) throws Exception {
        String url = cfg.get(Constants.LOG_TOKEN_URL);
        SimpleHttp request = SimpleHttp.doPost(url, session)
                .param("grant_type", "client_credentials")
                .param("client_id", cfg.get(Constants.LOG_CLIENT_ID))
                .param("client_secret", cfg.get(Constants.LOG_CLIENT_SECRET))
                .connectTimeoutMillis(Constants.CONNECT_TIMEOUT_MILLIS)
                .socketTimeOutMillis(Constants.SOCKET_TIMEOUT_MILLIS);

        try (SimpleHttp.Response response = request.asResponse()) {
            int status = response.getStatus();

            if (status != 200) {
                // Read body once for logging/exception
                String body = response.asString();
                LOG.errorf("Failed to get access token from %s: HTTP %d - %s", url, status, body);
                return null;
            }

            JsonNode json = response.asJson();
            if (json == null || json.get("access_token") == null || json.get("access_token").isNull()) {
                String body = response.asString();
                LOG.errorf("No access_token in response from %s. Body: %s", url, body);
                return null;
            }

            String token = json.get("access_token").asText();
            long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong() : 60L;
            tokenExpiryEpochSec.set(Instant.now().getEpochSecond() + expiresIn - 60); // refresh 60s early
            cachedToken = token;
            return token;
        } catch (Exception e) {
            LOG.warnf("Exception while fetching access token from %s: %s", url, e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> registerUserInLeap(UserModel user, AuthenticationSessionModel authSession) throws Exception {
        final long start = System.nanoTime();
        final String logPrefix = String.format("[LeapReg user=%s id=%s] ", user.getUsername(), user.getId());

        LOG.infof("%sStarting Leap registration", logPrefix);
        if (authSession == null) {
            LOG.infof("%sauthSession is null", logPrefix);
            throw new MissingResourceException("Authentication session is null", "AuthenticationSessionModel", "authSession");
        }

        Map<String, String> cfg = resolveLoggingConfig(session.getContext().getRealm());
        if (cfg == null) {
            LOG.warnf("%sLeap Logging Config not found; skipping", logPrefix);
            throw new MissingResourceException("Leap Logging Config not found", "AuthenticatorConfigModel", "Leap Logging Config");
        }

        String apiUrl = cfg.get(Constants.LEAP_REG_API_URL);
        if (StringUtil.isBlank(apiUrl)) {
            LOG.warnf("%sMissing %s in Leap Logging Config; skipping", logPrefix, Constants.LEAP_REG_API_URL);
            throw new MissingResourceException("Leap Registration API URL not configured", "AuthenticatorConfigModel", Constants.LEAP_REG_API_URL);
        }

        String invitationId = resolveInvitationId(authSession);
        LOG.infof("%sResolved InvitationId: %s", logPrefix, invitationId);
        ObjectNode payload;
        if (!StringUtil.isNullOrEmpty(invitationId)) {
            LOG.infof("%sRegistering user with InvitationId: %s", logPrefix, invitationId);
            payload = MAPPER.createObjectNode();
            payload.put("InvitationId", invitationId);
            payload.put("KeycloakId", user.getId());
        } else {
            payload = buildLeapRegistrationPayload(user, authSession);
        }

        // Build payload
        LOG.infof("%sPayload: %s", logPrefix, MAPPER.writeValueAsString(payload));

        // Auth
        String token = ensureToken(cfg);

        // First attempt
        HttpResult r1 = doPostJson(apiUrl, token, payload);
        if (r1.status == 401) {
            // Retry with refreshed token
            LOG.infof("%sToken expired; refreshing and retrying", logPrefix);
            token = refreshToken(cfg);
            HttpResult r2 = doPostJson(apiUrl, token, payload);

            logHttp(logPrefix, apiUrl, r2, "retry");
            if (is2xx(r2.status)) {
                LOG.infof("%sLeap registration success in retry in %dms", logPrefix, elapsedMs(start));
                return parseLeapResponse(r2.body);
            }
            throw new Exception(String.format("Leap registration failed with status %d (retry)", r2.status));
        }

        // Log first attempt
        logHttp(logPrefix, apiUrl, r1, "first");

        if (is2xx(r1.status)) {
            LOG.infof("%sLeap registration success in retry in %dms", logPrefix, elapsedMs(start));
            return parseLeapResponse(r1.body);
        }

        throw new Exception(String.format("Leap registration failed with status %d", r1.status));
    }

    private record HttpResult(int status, String body, long ms) {
    }

    private HttpResult doPostJson(String url, String bearerToken, ObjectNode json) throws Exception {
        long t0 = System.nanoTime();
        try (SimpleHttp.Response resp = SimpleHttp
                .doPost(url, session)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .json(json)
                .connectTimeoutMillis(Constants.CONNECT_TIMEOUT_MILLIS)
                .socketTimeOutMillis(Constants.SOCKET_TIMEOUT_MILLIS)
                .asResponse()) {

            int status = resp.getStatus();
            String body = safeRead(resp);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            return new HttpResult(status, body, ms);
        }
    }

    private void logHttp(String logPrefix, String url, HttpResult r, String attemptTag) {
        if (is2xx(r.status)) {
            LOG.infof("%sPOST %s -> %d in %dms (%s)", logPrefix, url, r.status, r.ms, attemptTag);
            if (LOG.isDebugEnabled())
                LOG.debugf("%sResponse body (%s): %s", logPrefix, attemptTag, truncate(r.body));
        } else {
            LOG.warnf("%sPOST %s -> %d in %dms; body: %s (%s)", logPrefix, url, r.status, r.ms, truncate(r.body), attemptTag);
        }
    }

    private boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }

    private long elapsedMs(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000;
    }

    private String safeRead(SimpleHttp.Response resp) {
        try {
            return resp.asString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> parseLeapResponse(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            // Parse JSON string into a Map
            Map<String, Object> userInfo = MAPPER.readValue(body, new TypeReference<>() {
            });
            LOG.infof("Parsed user info: %s", userInfo);
            return userInfo;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse user info JSON: %s", body);
            return Collections.emptyMap();
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() <= Constants.RESP_BODY_SIZE_MAX ? s : s.substring(0, Constants.RESP_BODY_SIZE_MAX) + "…";
    }

    /* Existing helpers from previous refactor (unchanged) */

    private ObjectNode buildLeapRegistrationPayload(UserModel user, AuthenticationSessionModel authSession) {
        String title = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.TITLE);
        ObjectNode root = MAPPER.createObjectNode();

        root.put("KeycloakId", user.getId());
        root.set("AuditTrail", buildAuditTrail());
        root.set("Identifier", buildIdentifierArray(authSession));

        String gender = resolveGender(user);
        root.put("Gender", gender);

        root.set("Name", buildNameArray(user.getFirstName(), user.getLastName(), gender, title));

        String phone = resolvePhone(user);
        root.set("Telecom", buildTelecomArray(user.getEmail(), phone, user.isEmailVerified()));

        root.set("Contact", MAPPER.createArrayNode());
        root.set("Communication", MAPPER.createArrayNode());

        root.set("Address", buildAddressArray(authSession));

        String birthDate = resolveBirthDate(user);
        if (!StringUtil.isBlank(birthDate)) root.put("BirthDate", birthDate);

        root.put("TimeZone", resolveTimeZone(user));
        return root;
    }

    private ArrayNode buildIdentifierArray(AuthenticationSessionModel authSession) {
        ArrayNode identifierArray = MAPPER.createArrayNode();
        String identifierSystem = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.IDENTIFIER_SYSTEM);
        String identifierUse = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.IDENTIFIER_USE);
        String identifierValue = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.IDENTIFIER_VALUE);

        if (StringUtil.isNotBlank(identifierSystem) && StringUtil.isNotBlank(identifierValue) && StringUtil.isNotBlank(identifierUse)) {
            ObjectNode objNode = MAPPER.createObjectNode();
            objNode.put("System", identifierSystem);
            objNode.put("Use", identifierUse);
            objNode.put("Value", identifierValue);
            identifierArray.add(objNode);
        }

        return identifierArray;
    }

    private ArrayNode buildAddressArray(AuthenticationSessionModel authSession) {
        ArrayNode addressArray = MAPPER.createArrayNode();
        String city = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.CITY);
        String state = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.STATE);
        String country = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.COUNTRY);
        String zipCode = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.ZIPCODE);
        String address1 = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.ADDRESS_1);
        String address2 = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.ADDRESS_2);

        if (StringUtil.isNotBlank(address1)) {
            addressArray.add(buildAddressObject(city, state, country, zipCode, address1));
        }

        if (StringUtil.isNotBlank(address2)) {
            addressArray.add(buildAddressObject(city, state, country, zipCode, address2));
        }

        return addressArray;
    }

    private ObjectNode buildAddressObject(String city, String state, String country, String zipCode, String address1) {
        ObjectNode addressObj = MAPPER.createObjectNode();
        addressObj.put("City", city);
        addressObj.put("State", state);
        addressObj.put("Country", country);
        addressObj.put("ZipCode", zipCode);
        addressObj.put("Address1", address1);
        return addressObj;
    }

    private ObjectNode buildAuditTrail() {
        ObjectNode audit = MAPPER.createObjectNode();
        audit.put("CreatedBySystem", "Leap");
        audit.put("LastChangedBySystem", "Leap");
        audit.put("WorkflowOrServiceName", "Leap_Web");
        return audit;
    }

    private String resolveGender(UserModel user) {
        String gender = safeFirstAttr(user, "gender");
        return StringUtil.isBlank(gender) ? "Unknown" : gender;
    }

    private ArrayNode buildNameArray(String first, String last, String gender, String title) {
        var arr = MAPPER.createArrayNode();
        ObjectNode nameObj = MAPPER.createObjectNode();

        String text = (first != null ? first : "") + " " + (last != null ? last : "");
        nameObj.put("Text", text.trim());
        nameObj.put("Family", last != null ? last : "");

        var givenArr = MAPPER.createArrayNode();
        if (!StringUtil.isBlank(first)) givenArr.add(first);
        nameObj.set("Given", givenArr);

        var prefixArr = MAPPER.createArrayNode();
        if (StringUtil.isNotBlank(title)) {
            prefixArr.add(title);
        } else {
            prefixArr.add(getNamePrefix(gender));
        }
        nameObj.set("Prefix", prefixArr);

        arr.add(nameObj);
        return arr;
    }

    private ArrayNode buildTelecomArray(String email, String phone, boolean emailVerified) {
        var arr = MAPPER.createArrayNode();
        if (!StringUtil.isBlank(email)) {
            ObjectNode tEmail = MAPPER.createObjectNode();
            tEmail.put("System", "Email");
            tEmail.put("Value", email);
            tEmail.put("IsVerified", emailVerified);
            arr.add(tEmail);
        }
        if (!StringUtil.isBlank(phone)) {
            String cleaned = getCleanPhoneValue(phone);
            if (!StringUtil.isBlank(cleaned)) {
                ObjectNode tPhone = MAPPER.createObjectNode();
                tPhone.put("System", "Phone");
                tPhone.put("Value", cleaned);
                tPhone.put("Use", "Mobile");
                arr.add(tPhone);
            }
        }
        return arr;
    }

    private String resolvePhone(UserModel user) {
        return safeFirstAttr(user, "mobile_number");
    }

    private String resolveBirthDate(UserModel user) {
        String bd = safeFirstAttr(user, "dob");
        if (StringUtil.isBlank(bd)) return null;
        return bd.contains("T") ? bd : (bd + "T00:00:00.000Z");
    }

    private String resolveTimeZone(UserModel user) {
        String tz = safeFirstAttr(user, "timeZone");
        return StringUtil.isBlank(tz) ? java.time.ZoneId.systemDefault().toString() : tz;
    }

    private String safeFirstAttr(UserModel user, String key) {
        try {
            String v = user.getFirstAttribute(key);
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    private String getNamePrefix(String gender) {
        return Constants.MALE.equals(gender) ? Constants.Mr
                : Constants.FEMALE.equals(gender) ? Constants.Mrs
                : Constants.Mx;
    }

    private String getCleanPhoneValue(String cell) {
        if (cell == null) return "";
        return cell.replaceAll("[^0-9]", "");
    }

    private String resolveInvitationId(AuthenticationSessionModel authSession) {
        String rawState = authSession.getClientNote(OIDCLoginProtocol.STATE_PARAM);
        LOG.info("Decoding InvitationId from state : " + rawState);
        JsonNode stateJson = StateDecoder.decodeState(rawState);
        JsonNode activationCode;
        if (null != stateJson) {
            LOG.infof("Decoded state JSON: %s", stateJson.toString());
            activationCode = stateJson.get(Constants.ACTIVATION_CODE_PROFILE);
            return StateDecoder.getString(activationCode, Constants.INVITATION_ID);
        }
        return null;
    }

    /**
     * Serialize payload and post. This method does network IO and should be called off the Keycloak session thread.
     */
    public void send(FailedEventLogRequest payload) {
        try {
            byte[] bodyBytes = MAPPER.writeValueAsBytes(payload);
            doPost(bodyBytes);
        } catch (Exception e) {
            LOG.error("Failed to send event payload", e);
        }
    }

    /**
     * Serialize payload and post. This method does network IO and should be called off the Keycloak session thread.
     */
    public void send(EventPayload payload) {
        try {
            byte[] bodyBytes = MAPPER.writeValueAsBytes(payload);
            doPost(bodyBytes);
        } catch (Exception e) {
            LOG.error("Failed to send event payload", e);
        }
    }

    private void doPost(byte[] bodyBytes) throws Exception {

        Map<String, String> cfg = resolveLoggingConfig(session.getContext().getRealm());
        if (cfg == null) return;

        String apiUrl = cfg.get(Constants.LEAP_LOG_FAILED_SESSION_URL);
        if (StringUtil.isBlank(apiUrl)) {
            LOG.warnf("Missing %s in Leap Logging Config; skipping session log post", Constants.LEAP_LOG_FAILED_SESSION_URL);
            return;
        }
        // ensure we have a token
        String token = ensureToken(cfg);
        if(StringUtil.isNullOrEmpty(token)) {
            LOG.warn("No access token available; skipping session log post");
            return;
        }

        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        // add auth header if needed, e.g. post.setHeader("Authorization", "Bearer ...");
        post.setEntity(new ByteArrayEntity(bodyBytes, ContentType.APPLICATION_JSON));

        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        LOG.infof("Posting session log to %s, payload size=%d bytes", apiUrl, bodyBytes.length);

        try (CloseableHttpResponse resp = httpClient.execute(post)) {
            int status = resp.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                LOG.infof("Posted session log, status=%d, respBody=%s", status, respBody);
            } else {
                String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                LOG.warnf("Non-2xx response when posting session log: status=%d response=%s", status, respBody);
            }
        } catch (Exception e) {
            // No retry as requested; just log
            LOG.errorf(e, "Failed to post session log (no retry). payload_size=%d", bodyBytes.length);
        }
    }

    public Map<String, Object> registerRelatedPatientInLeap(UserModel user, AuthenticationSessionModel authSession) throws Exception {
        final long start = System.nanoTime();
        final String logPrefix = String.format("[LeapReg user=%s id=%s] ", user.getUsername(), user.getId());

        LOG.infof("%sStarting Related Patient Leap registration", logPrefix);
        if (authSession == null) {
            LOG.infof("%sauthSession is null", logPrefix);
            throw new MissingResourceException("Authentication session is null", "AuthenticationSessionModel", "authSession");
        }

        Map<String, String> cfg = resolveLoggingConfig(session.getContext().getRealm());
        if (cfg == null) {
            LOG.warnf("%sLeap Logging Config not found; skipping", logPrefix);
            throw new MissingResourceException("Leap Logging Config not found", "AuthenticatorConfigModel", "Leap Logging Config");
        }

        String apiUrl = cfg.get(Constants.LEAP_RP_REG_API_URL);
        if (StringUtil.isBlank(apiUrl)) {
            LOG.warnf("%sMissing %s in Leap Logging Config; skipping", logPrefix, Constants.LEAP_RP_REG_API_URL);
            throw new MissingResourceException("Leap Registration API URL not configured", "AuthenticatorConfigModel", Constants.LEAP_RP_REG_API_URL);
        }

        ObjectNode payload = buildRelatedPatientPayload(user, authSession);
        // Build payload
        LOG.infof("%sPayload: %s", logPrefix, MAPPER.writeValueAsString(payload));

        // Auth
        String token = ensureToken(cfg);

        // First attempt
        HttpResult r1 = doPostJson(apiUrl, token, payload);
        if (r1.status == 401) {
            // Retry with refreshed token
            LOG.infof("%sToken expired; refreshing and retrying", logPrefix);
            token = refreshToken(cfg);
            HttpResult r2 = doPostJson(apiUrl, token, payload);

            logHttp(logPrefix, apiUrl, r2, "retry");
            if (is2xx(r2.status)) {
                LOG.infof("%sLeap registration success in retry in %dms", logPrefix, elapsedMs(start));
                return parseLeapResponse(r2.body);
            }
            throw new Exception(String.format("Leap registration failed with status %d (retry)", r2.status));
        }

        // Log first attempt
        logHttp(logPrefix, apiUrl, r1, "first");

        if (is2xx(r1.status)) {
            LOG.infof("%sLeap registration success in retry in %dms", logPrefix, elapsedMs(start));
            return parseLeapResponse(r1.body);
        }

        throw new Exception(String.format("Leap registration failed with status %d", r1.status));
    }

    private ObjectNode buildRelatedPatientPayload(UserModel user,
                                                  AuthenticationSessionModel authSession) {
        String title = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.TITLE);
        ObjectNode root = MAPPER.createObjectNode();

        root.set("LinkPatients", buildLinkPatientsArray(user, authSession));
        root.set("Identifier", buildIdentifierArray(authSession));

        String gender = resolveGender(user, authSession);
        root.put("Gender", gender);

        String birthDate = resolveBirthDate(user, authSession);
        if (!StringUtil.isBlank(birthDate)) {
            root.put("BirthDate", birthDate);
        }

        root.set("Name", buildNameArray(resolveFirstName(user, authSession), resolveLastName(user, authSession), gender, title));

        root.set("Address", buildAddressArray(authSession));
        root.set("AuditTrail", buildAuditTrail());

        String keycloakId = user != null ? user.getId() : null;
        if (!StringUtil.isBlank(keycloakId)) {
            root.put("KeycloakId", keycloakId);
        }

        return root;
    }

    private ArrayNode buildLinkPatientsArray(UserModel user,
                                             AuthenticationSessionModel authSession) {
        String title = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_TITLE);
        ArrayNode arr = MAPPER.createArrayNode();
        ObjectNode obj = MAPPER.createObjectNode();

        obj.set("Identifier", buildRpIdentifierArray(authSession));

        String gender = resolveGender(user, authSession);
        obj.put("Gender", gender);
        obj.put("Active", true);

//        obj.set("Name", buildNameArray(resolveFirstName(user, authSession), resolveLastName(user, authSession), gender, title));

        String phone = resolvePhone(user);
        obj.set("Telecom", buildTelecomArray(user.getEmail(), phone, user.isEmailVerified()));

        obj.set("Address", buildRPAddressArray(authSession));
        obj.set("AuditTrail", buildAuditTrail());
//        obj.set("Period", buildPeriod());

        arr.add(obj);
        return arr;
    }

    private ArrayNode buildRpIdentifierArray(AuthenticationSessionModel authSession) {
        ArrayNode arr = MAPPER.createArrayNode();
        String rpSystem = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_SYSTEM);
        String rpUse = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_USE);
        String rpValue = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_VALUE);

        if (StringUtil.isBlank(rpSystem) || StringUtil.isBlank(rpUse) || StringUtil.isBlank(rpValue)) {
            return arr;
        }
        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("System", rpSystem);
        obj.put("Use", rpUse);
        obj.put("Value", rpValue);
        arr.add(obj);
        return arr;
    }

    private ArrayNode buildRPAddressArray(AuthenticationSessionModel authSession) {
        ArrayNode addressArray = MAPPER.createArrayNode();
        String city = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_CITY);
        String state = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_STATE);
        String country = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_COUNTRY);
        String zipCode = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_ZIPCODE);
        String address1 = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_ADDRESS_1);
        String address2 = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_ADDRESS_2);

        if (StringUtil.isNotBlank(address1)) {
            addressArray.add(buildAddressObject(city, state, country, zipCode, address1));
        }

        if (StringUtil.isNotBlank(address2)) {
            addressArray.add(buildAddressObject(city, state, country, zipCode, address2));
        }

        return addressArray;
    }

    private ObjectNode buildPeriod() {
        ObjectNode p = MAPPER.createObjectNode();
        String now = Instant.now().toString();
        p.put("Start", now);
        p.put("End", now);
        return p;
    }

    private String resolveFirstName(UserModel user, AuthenticationSessionModel authSession) {
        return firstNonBlank(
                authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_FIRST_NAME),
                user != null ? user.getFirstName() : null,
                safeFirstAttr(user, "first_name")
        );
    }

    private String resolveLastName(UserModel user, AuthenticationSessionModel authSession) {
        return firstNonBlank(
                authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_LAST_NAME),
                user != null ? user.getLastName() : null,
                safeFirstAttr(user, "last_name")
        );
    }

    private String resolveGender(UserModel user, AuthenticationSessionModel authSession) {
        String gender = firstNonBlank(
                authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_GENDER),
                safeFirstAttr(user, "gender")
        );
        return StringUtil.isBlank(gender) ? "Unknown" : gender;
    }

    private String resolveBirthDate(UserModel user, AuthenticationSessionModel authSession) {
        String bd = firstNonBlank(
                authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_DOB),
                safeFirstAttr(user, "dob")
        );
        if (StringUtil.isBlank(bd)) return null;
        return bd.contains("T") ? bd : (bd + "T00:00:00.000Z");
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (!StringUtil.isBlank(v)) {
                return v;
            }
        }
        return null;
    }

    public boolean isRelatedPatientRegistration(AuthenticationSessionModel authSession) {
        if (authSession == null) {
            LOG.info("authSession is null");
            return false;
        }

        String rpSystem = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_SYSTEM);
        String rpUse = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_USE);
        String rpValue = authSession.getClientNote(Constants.CLIENT_REQUEST_PARAM_ + Constants.RP_IDENTIFIER_VALUE);

        return !StringUtil.isBlank(rpSystem) && !StringUtil.isBlank(rpUse) && !StringUtil.isBlank(rpValue);
    }

}
