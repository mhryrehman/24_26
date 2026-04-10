package com.keycloak.security.extensions.newdevice;

import com.keycloak.security.extensions.common.Constant;
import com.keycloak.security.extensions.common.DeviceInfo;
import com.keycloak.security.extensions.common.NotificationService;
import com.keycloak.security.extensions.common.Utils;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: Yasir Rehman
 * <p>
 * Authenticator that checks whether a login attempt originates from a known device.
 * This authenticator is typically placed in the Browser flow after user authentication succeeds.
 */
public class NewDeviceAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(NewDeviceAuthenticator.class);
    private static final String CLIENT_REQUEST_PARAM_PREFIX = "client_request_param_";

    /**
     * Main entry point: checks device ID against user attribute, sends email for new devices, and persists the ID.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("DeviceAddressAuthenticator.authenticate called");
        log.info("PAR parameter : " + context.getAuthenticationSession().getClientNote("client_request_param_ehr"));

        context.getHttpRequest().getHttpHeaders().getRequestHeaders().forEach(
                (name, values) -> System.out.println(name + " = " + values)
        );

        String userAgent = context.getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.USER_AGENT);
        log.info("User-Agent: " + userAgent);

        DeviceInfo deviceInfo = prepareDeviceInfo(userAgent);
        String fpKey = deviceInfo.fingerprint();
        String deviceId = sha256(fpKey);
        log.info("Normalized UA: " + fpKey);
        log.info("Device ID (SHA-256): " + deviceId);


        UserModel user = context.getUser();
        if (user == null) {
            context.success();
            return;
        }

        String userAttrName = Utils.getConfigString(
                context.getAuthenticatorConfig(),
                Constant.USER_ATTRIBUTE_NAME,
                Constant.DEVICE_ADDRESSES
        );
        String deviceParamName = Utils.getConfigString(
                context.getAuthenticatorConfig(),
                Constant.DEVICE_QUERY_PARAM_NAME,
                Constant.DEVICE_ID
        );

//        final String deviceId = resolveDeviceId(context, deviceParamName);
        log.infof("Extracted deviceId: %s", Utils.mask(deviceId));

        if (StringUtil.isBlank(deviceId)) {
            context.success();
            return;
        }

        final String norm = deviceId.trim().toLowerCase(Locale.ROOT);
        final Set<String> knownDevices = csvToSet(user.getFirstAttribute(userAttrName));

        final boolean firstLogin = knownDevices.isEmpty();
        final boolean alreadyKnown = knownDevices.contains(norm);

        if (!firstLogin && !alreadyKnown) {
            try {
                deviceInfo.setIpAddress(context.getConnection().getRemoteAddr());
                NotificationService.sendNewDeviceEmail(context, deviceInfo);
                log.infof("Sent new-device email for user [%s], deviceId=%s", user.getUsername(), deviceId);
            } catch (Exception e) {
                log.warnf(e, "Failed to send new-device email for user %s", user.getUsername());
            }
            knownDevices.add(norm);
            user.setSingleAttribute(userAttrName, setToCsv(knownDevices));
        }

        if (firstLogin) {
            knownDevices.add(norm);
            user.setSingleAttribute(userAttrName, setToCsv(knownDevices));
        }

        context.success();
    }

    /**
     * Called when an authenticator requires user input, not used here.
     */
    @Override
    public void action(AuthenticationFlowContext context) {
    }

    /**
     * Indicates that this authenticator requires a user to be set in the context.
     */
    @Override
    public boolean requiresUser() {
        return true;
    }

    /**
     * Checks if the authenticator is configured for the given user; always true here.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    /**
     * Adds any required actions for the user; not used in this authenticator.
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    /**
     * Called on shutdown/cleanup of the authenticator; no-op here.
     */
    @Override
    public void close() {
    }

    /**
     * Converts a CSV string of device IDs to a normalized (lowercased) Set.
     */
    private static Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Converts a Set of device IDs back to a CSV string.
     */
    private static String setToCsv(Set<String> set) {
        return set.stream().collect(Collectors.joining(","));
    }

    /**
     * Prefer form param for Direct Access Grant; otherwise AuthenticationSession note.
     */
    private String resolveDeviceId(AuthenticationFlowContext context, String paramName) {
        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
        if (!Utils.isNullOrEmpty(formParams) && Constant.PASSWORD.equals(formParams.getFirst(Constant.GRANT_TYPE))) {
            String v = formParams.getFirst(paramName);
            if (!StringUtil.isBlank(v)) return v;
        }

        String noteKey = CLIENT_REQUEST_PARAM_PREFIX + paramName;
        return context.getAuthenticationSession() != null ? context.getAuthenticationSession().getClientNote(noteKey) : null;
    }

    private DeviceInfo prepareDeviceInfo(String ua) {
        if (ua == null) return new DeviceInfo();
        String uaL = ua.toLowerCase(Locale.ROOT);
        String deviceType = uaL.contains("mobile") ? "mobile" : "desktop";
        String os = uaL.contains("windows") ? "Windows" :
                uaL.contains("mac os") ? "macOS" :
                        uaL.contains("android") ? "Android" :
                                uaL.contains("iphone") || uaL.contains("ipad") ? "iOS" : "Other";
        String osMajor = uaL.contains("windows nt 10") ? "10" :
                uaL.contains("windows nt 11") ? "11" : "";
        String arch = (uaL.contains("x64") || uaL.contains("x86_64")) ? "x64"
                : (uaL.contains("arm64") ? "arm64" : "");

        String family = uaL.contains("edg/") ? "Edge"
                : uaL.contains("chrome/") ? "Chrome"
                : uaL.contains("firefox/") ? "Firefox"
                : uaL.contains("safari/") ? "Safari" : "Other";

        String versionMajor = extractMajor(uaL, family);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceType(deviceType);
        deviceInfo.setOs(os);
        deviceInfo.setOsMajor(osMajor);
        deviceInfo.setBrowserFamily(family);
        deviceInfo.setBrowserVersionMajor(versionMajor);
        deviceInfo.setArch(arch);
        deviceInfo.setUaRaw(ua);

        return deviceInfo;
    }

    private String extractMajor(String uaL, String family) {
        try {
            String token = switch (family) {
                case "Edge" -> "edg/";
                case "Chrome" -> "chrome/";
                case "Firefox" -> "firefox/";
                default -> "";
            };
            if (!token.isEmpty()) {
                int i = uaL.indexOf(token);
                if (i >= 0) {
                    String tail = uaL.substring(i + token.length());
                    return tail.split("\\.")[0].replaceAll("[^0-9]", "");
                }
            }
        } catch (Exception ignore) {}
        return "";
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
