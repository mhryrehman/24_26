package com.keycloak.otp.auth.condition;

import com.keycloak.otp.util.Utils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.keycloak.otp.auth.condition.SessionNoteConditionAuthenticatorFactory.CFG_MATCH_ON_TRUTHY;
import static com.keycloak.otp.auth.condition.SessionNoteConditionAuthenticatorFactory.CFG_NOTE_KEY;
import static com.keycloak.otp.auth.condition.SessionNoteConditionAuthenticatorFactory.CFG_TRUTHY_VALUES;

/**
 * Generic session-note condition.
 * Matches depending on whether the authentication session note's value
 * is contained in a configured CSV list ("accepted values").
 *
 * Behavior:
 * - Build the set of accepted values from CFG_TRUTHY_VALUES (CSV, case-insensitive compare).
 * - Read the note (CFG_NOTE_KEY) from the authentication session.
 * - isAccepted := (note value exists && lowercased note is in accepted set)
 * - Result := matchWhenAccepted ? isAccepted : !isAccepted
 *
 * Typical use:
 * - Fallback path: set CFG_MATCH_ON_TRUTHY = false (default) so it matches when note is NOT accepted.
 * - Positive path: set CFG_MATCH_ON_TRUTHY = true so it matches when note IS accepted.
 */
public class SessionNoteConditionAuthenticator implements ConditionalAuthenticator {

    public static final SessionNoteConditionAuthenticator SINGLETON = new SessionNoteConditionAuthenticator();
    private static final Logger LOG = Logger.getLogger(SessionNoteConditionAuthenticator.class);

    private SessionNoteConditionAuthenticator() { }

    @Override
    public boolean matchCondition(final AuthenticationFlowContext context) {
        final AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        final RealmModel realm = context.getRealm();
        final AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (configModel == null || configModel.getConfig() == null) {
            LOG.info("SessionNoteCondition: no config model; returning FALSE.");
            return false;
        }

        // Required: note key
        final String noteKey = safeTrim(configModel.getConfig().get(CFG_NOTE_KEY));
        if (noteKey == null || noteKey.isEmpty()) {
            LOG.info("SessionNoteCondition: no note key configured; returning FALSE.");
            return false;
        }

        // Accepted values (CSV). Default to "true" if not provided.
        final String csv = safeTrim(configModel.getConfig().getOrDefault(CFG_TRUTHY_VALUES, "true"));
        final Set<String> acceptedValues = csvToLowerSet(csv);

        // Should this condition match when the note is in the accepted set?
        final boolean matchWhenAccepted = readBoolean(configModel, CFG_MATCH_ON_TRUTHY, true);

        // No auth session => treat as "no note" -> not accepted
        if (authSession == null) {
            LOG.info("SessionNoteCondition: no auth session; treating note as absent.");
            return !matchWhenAccepted;
        }

        final String rawValue = safeTrim(authSession.getAuthNote(noteKey));
        final boolean isAccepted = rawValue != null && acceptedValues.contains(rawValue.toLowerCase());

        final boolean result = matchWhenAccepted ? isAccepted : !isAccepted;
        logDecision(realm, noteKey, rawValue, acceptedValues, matchWhenAccepted, result);
        return result;
    }

    @Override
    public void action(final AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(final org.keycloak.models.KeycloakSession session,
                                   final org.keycloak.models.RealmModel realm,
                                   final org.keycloak.models.UserModel user) {
        // no-op
    }

    @Override
    public void close() { }

    // ----------------- helpers -----------------

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static Set<String> csvToLowerSet(String csv) {
        if (csv == null || csv.isEmpty()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Reads a boolean from the config. Prefers Keycloak's boolean semantics.
     * Falls back to OtpUtils.getConfigBool if present; otherwise uses Boolean.parseBoolean.
     */
    private static boolean readBoolean(AuthenticatorConfigModel configModel, String key, boolean defaultVal) {
        final String raw = configModel.getConfig().get(key);
        if (raw == null) return defaultVal;
        try {
            // If you prefer only strict "true"/"false" from KC's BOOLEAN_TYPE:
            return Boolean.parseBoolean(raw);
        } catch (Exception e) {
            // optional: keep legacy helper if you use it elsewhere
            try {
                return Utils.getConfigBool(configModel, key, defaultVal);
            } catch (Throwable ignore) {
                return defaultVal;
            }
        }
    }

    private static void logDecision(RealmModel realm,
                                    String noteKey,
                                    String noteValue,
                                    Set<String> acceptedValues,
                                    boolean matchWhenAccepted,
                                    boolean result) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("SessionNoteCondition eval: realm=%s noteKey=%s noteValue=%s accepted=%s matchWhenAccepted=%s => match=%s",
                    realm != null ? realm.getName() : "unknown",
                    noteKey, noteValue, acceptedValues, matchWhenAccepted, result);
        } else {
            // Keep INFO succinct; switch to DEBUG above for full details
            LOG.infof("SessionNoteCondition: key=%s match=%s", noteKey, result);
        }
    }
}
