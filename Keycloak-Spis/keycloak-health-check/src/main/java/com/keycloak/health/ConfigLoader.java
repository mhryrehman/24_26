package com.keycloak.health;

import org.jboss.logging.Logger;
import org.keycloak.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class ConfigLoader {

    private static final Logger LOG = Logger.getLogger(ConfigLoader.class);

    // ---- Keys ----
    private static final String K_BASE_URL = "health-checker-keycloak-base-url";
    private static final String K_TOKEN_URI = "health-checker-token-uri";
    private static final String K_REALM = "health-checker-realm";
    private static final String K_CLIENT_ID = "health-checker-client-id";
    private static final String K_CLIENT_SECRET = "health-checker-client-secret";
    private static final String K_TEST_USER = "health-checker-test-username";
    private static final String K_TEST_PASS = "health-checker-test-password";
    private static final String K_SUBJECT_TOKEN = "health-checker-subject-token";
    private static final String K_SUBJECT_ISS = "health-checker-subject-issuer";

    // Config file discovery
    private static final String SCOPE_CONF_KEY = "config-file";
    private static final String ENV_CONF_KEY = "KEYCLOAK_SPI_CONF";
    private static final String DEFAULT_CONF_PATH = "/opt/bitnami/keycloak/conf/keycloak-spi.conf";
    private static final String fallbackPath = "/opt/bitnami/keycloak/conf/keycloak.conf";

    private static volatile Properties props;

    // ---- Cached fields ----
    private static volatile String keycloakBaseUrl;
    private static volatile String keycloakTokenUri;
    private static volatile String realm;
    private static volatile String clientId;
    private static volatile String clientSecret;
    private static volatile String testUsername;
    private static volatile String testPassword;
    private static volatile String subjectToken;
    private static volatile String subjectIssuer;

    private ConfigLoader() { /* no-op */ }

    public static void init(Config.Scope config) {
        if (props != null) return;
        synchronized (ConfigLoader.class) {
            if (props != null) return;

            String pathStr = resolveConfigPath(config);
            props = new Properties();

            try (FileInputStream fis = new FileInputStream(pathStr)) {
                props.load(fis);
                LOG.infof(">>> [SPI] Loaded config from %s", pathStr);
            } catch (IOException e) {
                LOG.warnf(">>> [SPI] Could not load config from %s (%s). Falling back to keycloak.conf", pathStr, e.getMessage());
                // fallback: use main keycloak.conf
                try (FileInputStream fis = new FileInputStream(fallbackPath)) {
                    props.load(fis);
                    LOG.infof(">>> [SPI] Loaded fallback config from %s", fallbackPath);
                } catch (IOException ex2) {
                    LOG.errorf(">>> [SPI] Failed to load fallback config from %s: %s", fallbackPath, ex2.getMessage());
                    props = new Properties(); // keep non-null, but empty
                }
            }

            keycloakBaseUrl = props.getProperty(K_BASE_URL);
            keycloakTokenUri = props.getProperty(K_TOKEN_URI);
            realm = props.getProperty(K_REALM);
            clientId = props.getProperty(K_CLIENT_ID);
            clientSecret = props.getProperty(K_CLIENT_SECRET);
            testUsername = props.getProperty(K_TEST_USER);
            testPassword = props.getProperty(K_TEST_PASS);
            subjectToken = props.getProperty(K_SUBJECT_TOKEN);
            subjectIssuer = props.getProperty(K_SUBJECT_ISS);
            printProps();
        }
    }

    private static String resolveConfigPath(Config.Scope config) {
        if (config != null) {
            String fromScope = config.get(SCOPE_CONF_KEY);
            LOG.debug("Config.Scope 'config-file' = " + fromScope);
            if (fromScope != null && !fromScope.isBlank()) {
                return fromScope.trim();
            }
        }

        String fromEnv = System.getenv(ENV_CONF_KEY);
        LOG.debug("Environment 'KEYCLOAK_SPI_CONF' = " + fromEnv);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        LOG.debug("Environment 'KEYCLOAK_SPI_CONF' not set, using default path");
        return DEFAULT_CONF_PATH;
    }

    private static void ensureInit() {
        if (props == null) {
            init(null);
        }
    }

    // ---- Generic accessors ----
    public static String get(String key) {
        ensureInit();
        return props.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        ensureInit();
        return props.getProperty(key, defaultValue);
    }

    private static void printProps() {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (key.toLowerCase().contains("secret") || key.toLowerCase().contains("password")) {
                value = "*****";
            }
            LOG.debugf(">>> [SPI]   %s = %s", key, value);
        }
    }

    public static String keycloakBaseUrl() {
        ensureInit();
        return keycloakBaseUrl;
    }

    public static String keycloakTokenUri() {
        ensureInit();
        return keycloakTokenUri;
    }

    public static String realm() {
        ensureInit();
        return realm;
    }

    public static String clientId() {
        ensureInit();
        return clientId;
    }

    public static String clientSecret() {
        ensureInit();
        return clientSecret;
    }

    public static String testUsername() {
        ensureInit();
        return testUsername;
    }

    public static String testPassword() {
        ensureInit();
        return testPassword;
    }

    public static String subjectToken() {
        ensureInit();
        return subjectToken;
    }

    public static String subjectIssuer() {
        ensureInit();
        return subjectIssuer;
    }
}
