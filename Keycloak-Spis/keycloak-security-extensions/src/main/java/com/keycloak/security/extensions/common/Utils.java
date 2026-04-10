package com.keycloak.security.extensions.common;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.utils.StringUtil;

/**
 * Small, stateless helpers used by the authenticators.
 */
public final class Utils {

    private static final Logger log = Logger.getLogger(Utils.class);

    private Utils() {}

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
     * Returns true if the map is null or has no entries.
     *
     * @param map the map to check (may be null)
     * @return true if map == null or map.isEmpty(); false otherwise
     */
    public static boolean isNullOrEmpty(MultivaluedMap<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Masks an identifier for safe logging.
     *
     * @param id the original id (may be null)
     * @return masked string as described above
     */
    public static String mask(String id) {
        if (id == null) return "null";
        if (id.length() <= 6) return "****";
        return id.substring(0, 4) + "****" + id.substring(id.length() - 2);
    }
}
