package com.keycloak.otp.auth.conditional;

import com.keycloak.otp.auth.browser.OtpAuthenticatorForm;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.keycloak.otp.auth.conditional.ConditionalEmailAuthenticatorForm.OtpDecision.*;
import static org.keycloak.models.utils.KeycloakModelUtils.getRoleFromString;

/**
 * Author: Yasir Rehman
 * <p>
 * Conditional version of {@link OtpAuthenticatorForm} that dynamically determines
 * whether to require an OTP based on user attributes, roles, request headers, or default behavior.
 */
public class ConditionalEmailAuthenticatorForm extends OtpAuthenticatorForm {

    /**
     * Value to skip OTP challenge
     */
    public static final String SKIP = "skip";

    /**
     * Value to force OTP challenge
     */
    public static final String FORCE = "force";

    /**
     * Config key: User attribute used to control OTP requirement
     */
    public static final String OTP_CONTROL_USER_ATTRIBUTE = "otpControlAttribute";

    /**
     * Config key: Role name to skip OTP
     */
    public static final String SKIP_OTP_ROLE = "skipOtpRole";

    /**
     * Config key: Role name to force OTP
     */
    public static final String FORCE_OTP_ROLE = "forceOtpRole";

    /**
     * Config key: Header pattern to skip OTP
     */
    public static final String SKIP_OTP_FOR_HTTP_HEADER = "noOtpRequiredForHeaderPattern";

    /**
     * Config key: Header pattern to force OTP
     */
    public static final String FORCE_OTP_FOR_HTTP_HEADER = "forceOtpForHeaderPattern";

    /**
     * Config key: Default OTP decision when no conditions match
     */
    public static final String DEFAULT_OTP_OUTCOME = "defaultOtpOutcome";

    /**
     * Entry point for authentication. Evaluates conditions to decide whether OTP should be shown.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (tryConcludeBasedOn(voteForUserOtpControlAttribute(context.getUser(), config), context)) return;

        if (tryConcludeBasedOn(voteForUserRole(context.getRealm(), context.getUser(), config), context)) return;

        if (tryConcludeBasedOn(voteForHttpHeaderMatchesPattern(context.getHttpRequest().getHttpHeaders().getRequestHeaders(), config), context))
            return;

        if (tryConcludeBasedOn(voteForDefaultFallback(config), context)) return;

        showOtpForm(context);
    }

    /**
     * Evaluates default fallback behavior if no specific condition matched.
     */
    private OtpDecision voteForDefaultFallback(Map<String, String> config) {
        if (!config.containsKey(DEFAULT_OTP_OUTCOME)) return ABSTAIN;

        switch (config.get(DEFAULT_OTP_OUTCOME)) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    /**
     * Applies OTP behavior decision and concludes flow accordingly.
     */
    private boolean tryConcludeBasedOn(OtpDecision state, AuthenticationFlowContext context) {
        switch (state) {
            case SHOW_OTP:
                showOtpForm(context);
                return true;
            case SKIP_OTP:
                context.success();
                return true;
            default:
                return false;
        }
    }

    /**
     * Invokes the parent class to display OTP form and send OTP.
     */
    private void showOtpForm(AuthenticationFlowContext context) {
        super.authenticate(context);
    }

    /**
     * Evaluates user attribute to decide OTP requirement.
     */
    private OtpDecision voteForUserOtpControlAttribute(UserModel user, Map<String, String> config) {
        if (!config.containsKey(OTP_CONTROL_USER_ATTRIBUTE)) return ABSTAIN;

        String attributeName = config.get(OTP_CONTROL_USER_ATTRIBUTE);
        if (attributeName == null) return ABSTAIN;

        Optional<String> value = user.getAttributeStream(attributeName).findFirst();
        if (!value.isPresent()) return ABSTAIN;

        switch (value.get().trim()) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    /**
     * Evaluates request headers against configured patterns to decide OTP behavior.
     */
    private OtpDecision voteForHttpHeaderMatchesPattern(MultivaluedMap<String, String> requestHeaders, Map<String, String> config) {
        if (!config.containsKey(FORCE_OTP_FOR_HTTP_HEADER) && !config.containsKey(SKIP_OTP_FOR_HTTP_HEADER)) {
            return ABSTAIN;
        }

        if (containsMatchingRequestHeader(requestHeaders, config.get(SKIP_OTP_FOR_HTTP_HEADER))) {
            return SKIP_OTP;
        }

        if (containsMatchingRequestHeader(requestHeaders, config.get(FORCE_OTP_FOR_HTTP_HEADER))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    /**
     * Checks if any request header matches the given regex pattern.
     */
    private boolean containsMatchingRequestHeader(MultivaluedMap<String, String> requestHeaders, String headerPattern) {
        if (headerPattern == null) return false;

        Pattern pattern = Pattern.compile(headerPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                String headerEntry = key.trim() + ": " + value.trim();
                if (pattern.matcher(headerEntry).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluates user's roles to determine OTP requirement.
     */
    private OtpDecision voteForUserRole(RealmModel realm, UserModel user, Map<String, String> config) {
        if (!config.containsKey(SKIP_OTP_ROLE) && !config.containsKey(FORCE_OTP_ROLE)) return ABSTAIN;

        if (userHasRole(realm, user, config.get(SKIP_OTP_ROLE))) {
            return SKIP_OTP;
        }

        if (userHasRole(realm, user, config.get(FORCE_OTP_ROLE))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    /**
     * Checks if the user has the given role in the realm.
     */
    private boolean userHasRole(RealmModel realm, UserModel user, String roleName) {
        if (roleName == null) return false;

        RoleModel role = getRoleFromString(realm, roleName);
        return role != null && user.hasRole(role);
    }

    /**
     * Enum representing possible decisions whether to skip, show, or abstain from OTP.
     */
    enum OtpDecision {
        SKIP_OTP, SHOW_OTP, ABSTAIN
    }
}
