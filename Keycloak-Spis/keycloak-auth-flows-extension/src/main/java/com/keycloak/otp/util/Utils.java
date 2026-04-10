package com.keycloak.otp.util;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Small, stateless helpers used by the Email OTP authenticator.
 * Centralizes masking, brute-force bookkeeping, and safe config parsing.
 */
public final class Utils {

    private static final Logger log = Logger.getLogger(Utils.class);

    private Utils() {}

    /**
     * Masks the local-part of an email address, preserving the first 2 and last 2 characters.
     * If the local-part is too short or the input is invalid, the original value is returned.
     */
    public static String maskEmail(String email) {
        if (StringUtil.isNullOrEmpty(email) || !email.contains("@")) return email;
        final int at = email.indexOf('@');
        final String name = email.substring(0, at);
        final String domain = email.substring(at);

        if (name.length() <= 2) return name + domain;

        final String firstTwo = name.substring(0, 2);
        final int maskLen = Math.max(name.length() - firstTwo.length(), 1);
        return firstTwo + "*".repeat(maskLen) + domain;
    }

    /**
     * Returns only the last 4 characters of a phone number.
     * If null/blank, returns the input as-is (null stays null).
     */
    public static String maskMobileNumber(String mobileNumber) {
        if (StringUtil.isNullOrEmpty(mobileNumber)) return mobileNumber;
        final int len = mobileNumber.length();
        return len <= 4 ? mobileNumber : mobileNumber.substring(len - 4);
    }

//    /**
//     * Records a failed login attempt for brute-force detection.
//     * Updates the failure store and notifies the BruteForceProtector so Keycloak can enforce wait/lock.
//     */
//    public static void recordBruteForceFailure(AuthenticationFlowContext context, UserModel user) {
//        try {
//            final UserLoginFailureProvider failures = context.getSession().loginFailures();
//            UserLoginFailureModel f = failures.getUserLoginFailure(context.getRealm(), user.getId());
//            if (f == null) {
//                f = failures.addUserLoginFailure(context.getRealm(), user.getId());
//            }
//            f.incrementFailures();
//
//            // IMPORTANT: notify the protector so the disable/wait logic is applied
//            context.getProtector().failedLogin(context.getRealm(), user, context.getConnection());
//        } catch (Exception e) {
//            log.warnf(e, "Failed to record brute-force failure for user [%s]", user != null ? user.getUsername() : "null");
//        }
//    }

//    /**
//     * Clears the brute-force failure record for the user and (optionally) notifies the protector of a successful login.
//     */
//    public static void clearBruteForceFailures(AuthenticationFlowContext context, UserModel user) {
//        try {
//            context.getSession().loginFailures().removeUserLoginFailure(context.getRealm(), user.getId());
//            // Optional but tidy: let the protector know this login succeeded
//            context.getProtector().successfulLogin(context.getRealm(), user, context.getConnection());
//        } catch (Exception e) {
//            log.warnf(e, "Failed to clear brute-force failures for user [%s]", user != null ? user.getUsername() : "null");
//        }
//    }

    /**
     * Returns true if the user is currently considered disabled by brute-force detection
     * for the realm (delegates to AuthenticatorUtils).
     */
    public static boolean isUserLocked(AuthenticationFlowContext context, UserModel user) {
        if (!context.getRealm().isBruteForceProtected()) return false;
        return AuthenticatorUtils.getDisabledByBruteForceEventError(context, user) != null;
    }

    /**
     * Removes all OTP-related auth notes from the authentication session:
     * CODE, CODE_TTL, RESEND_COUNT, RESEND_LAST_TS (if used).
     */
    public static void clearAllOtpNotes(AuthenticationFlowContext context) {
        final AuthenticationSessionModel s = context.getAuthenticationSession();
        s.removeAuthNote(Constants.CODE);
        s.removeAuthNote(Constants.CODE_TTL);
        s.removeAuthNote(Constants.RESEND_COUNT);
        s.removeAuthNote(Constants.INVALID_ATTEMPTS);
    }

    /**
     * Parses a {@code long} from a string with a caller-provided default.
     * Null/blank/invalid values return the default.
     */
    public static long convertToLong(String input, long defaultValue) {
        if (StringUtil.isNullOrEmpty(input)) return defaultValue;
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            log.warnf("Invalid long: [%s]. Using default: %d", input, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses an {@code int} from a string with a caller-provided default.
     * Null/blank/invalid values return the default.
     */
    public static int convertToInt(String input, int defaultValue) {
        if (StringUtil.isNullOrEmpty(input)) return defaultValue;
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            log.warnf("Invalid int: [%s]. Using default: %d", input, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely read an integer config value from an AuthenticatorConfigModel.
     * Returns {@code defaultValue} if the config/model/map is null, the value is missing/blank, or invalid.
     */
    public static int getConfigInt(AuthenticatorConfigModel config, String key, int defaultValue) {
        if (config == null || config.getConfig() == null) return defaultValue;
        final String raw = config.getConfig().get(key);
        if (StringUtil.isNullOrEmpty(raw)) return defaultValue;
        return convertToInt(raw, defaultValue);
    }

    /**
     * Safely read a boolean config value from an AuthenticatorConfigModel.
     * Returns {@code defaultValue} if missing; uses {@link Boolean#parseBoolean(String)} semantics.
     */
    public static boolean getConfigBool(AuthenticatorConfigModel config, String key, boolean defaultValue) {
        if (config == null || config.getConfig() == null) return defaultValue;
        final String raw = config.getConfig().get(key);
        if (raw == null) return defaultValue;
        return Boolean.parseBoolean(raw.trim());
    }

    /**
     * Safely read a string config value from an AuthenticatorConfigModel.
     * Trims whitespace; returns {@code defaultValue} if missing or blank.
     */
    public static String getConfigString(AuthenticatorConfigModel config, String key, String defaultValue) {
        if (config == null || config.getConfig() == null) return defaultValue;
        final String raw = config.getConfig().get(key);
        return (StringUtil.isNullOrEmpty(raw)) ? defaultValue : raw.trim();
    }

    /**
     * Constant-time string comparison to mitigate timing side-channels.
     * Returns false if either input is null or lengths differ.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }


    public static String getStringFromObject(Object v) {
        return (v == null) ? null : String.valueOf(v);
    }

    public static Boolean getBooleanFromObject(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v);
        return "true".equalsIgnoreCase(s) ? Boolean.TRUE :
                "false".equalsIgnoreCase(s) ? Boolean.FALSE : null;
    }

    public static List<String> getListFromObject(Object v) {
        if (v == null) return Collections.emptyList();
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object e : list) if (e != null) out.add(String.valueOf(e));
            return out;
        }
        // single value fallback
        return List.of(String.valueOf(v));
    }

    public static boolean phoneAlreadyExists(KeycloakSession session, RealmModel realm, String phone) {
        // Attribute search. This is simple; for large user bases you may want indexed storage or custom lookup.
        UserModel userModel = session.users().getUserByUsername(realm, phone);
        if (userModel != null) {
            return true;
        }
        return session.users()
                .searchForUserByUserAttributeStream(realm, Constants.MOBILE_NUMBER, phone)
                .findAny()
                .isPresent();
    }

    public static UserModel findUserByPhone(AuthenticationFlowContext context, String phone) {
        return context.getSession().users()
                .searchForUserByUserAttributeStream(context.getRealm(), Constants.MOBILE_NUMBER, phone)
                .findFirst()
                .orElse(null);
    }

    public static boolean isEmail(String input) {
        if (input == null) {
            return false;
        }
        String value = input.trim();

        return value.contains("@") && value.indexOf('@') > 0 && value.indexOf('@') < value.length() - 1;
    }


    public static String normalizePhone(String phone) {
        // Minimal normalization (trim). Add libphonenumber if you want real normalization.
        return phone.trim();
    }

    public static boolean isPhone(String value) {
        final Pattern PHONE_PATTERN = Pattern.compile(Constants.PHONE_NUMBER_REGEX);
        return PHONE_PATTERN.matcher(normalizePhone(value)).matches();
    }

    public static String buildFullName(String firstName,
                                       String lastName,
                                       String username) {

        String f = firstName != null ? firstName.trim() : "";
        String l = lastName != null ? lastName.trim() : "";

        if (!f.isEmpty() && !l.isEmpty()) {
            return f + " " + l;
        }

        if (!f.isEmpty()) {
            return f;
        }

        if (!l.isEmpty()) {
            return l;
        }

        return username != null ? username : "";
    }

    public static String resolveOtpMethod(UserModel user) {
        if(user == null) return null;
        // For this example, we check for a mobile number attribute first, then email. Adjust as needed.
        boolean isEmailOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED));
        boolean isSmsOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_SMS_ENABLED));
        if (isEmailOtpEnabled && isSmsOtpEnabled) {
            return Constants.BOTH;
        } else if (isEmailOtpEnabled) {
            return Constants.EMAIL;
        } else if (isSmsOtpEnabled) {
            return Constants.MOBILE_NUMBER;
        }
        return null; // or a default method if you have one
    }

    public static String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public static void setClientIpInAuthSession(String clientIp, AuthenticationSessionModel session) {
        if (clientIp == null || session == null) {
            return;
        }

        session.setAuthNote(Constants.NOTE_CLIENT_IP, clientIp);
    }

    public static String getClientIpFromAuthSession(AuthenticationSessionModel session) {
        if (session == null) {
            return null;
        }

        return session.getAuthNote(Constants.NOTE_CLIENT_IP);
    }

}
