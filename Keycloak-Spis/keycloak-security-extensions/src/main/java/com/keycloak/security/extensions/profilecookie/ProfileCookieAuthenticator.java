package com.keycloak.security.extensions.profilecookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keycloak.security.extensions.common.Constant;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProfileCookieAuthenticator extends AbstractUsernameFormAuthenticator {

    private static final Logger log = Logger.getLogger(ProfileCookieAuthenticator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            log.warn("No user available in context. Skipping authentication.");
            context.attempted();
            return;
        }

        log.infof("Processing auto-profile cookie for user: %s", user.getUsername());
        Response response = buildResponseWithProfileCookie(context, user);
        context.challenge(response);
    }

    private Response buildResponseWithProfileCookie(AuthenticationFlowContext context, UserModel user) {
        Response formResponse = context.form().createForm(Constant.FORM_TEMPLATE);

        Map<String, String> config = context.getAuthenticatorConfig() != null
                ? context.getAuthenticatorConfig().getConfig()
                : Collections.emptyMap();

        String cookieName = config.getOrDefault(Constant.PROP_COOKIE_NAME, Constant.DEFAULT_COOKIE_NAME);
        String cookiePath = config.getOrDefault(Constant.PROP_COOKIE_PATH, Constant.DEFAULT_COOKIE_PATH);
        String cookieComment = config.getOrDefault(Constant.PROP_COOKIE_COMMENT, Constant.DEFAULT_COOKIE_COMMENT);
        boolean httpOnly = Boolean.parseBoolean(config.getOrDefault(Constant.PROP_HTTP_ONLY, Constant.DEFAULT_HTTP_ONLY));
        int maxAge = Integer.parseInt(config.getOrDefault(Constant.PROP_COOKIE_MAX_AGE, Constant.DEFAULT_COOKIE_MAX_AGE));

        List<Map<String, String>> profiles = loadExistingProfiles(context, cookieName);
        addUserProfileIfAbsent(profiles, user);

        try {
            String json = OBJECT_MAPPER.writeValueAsString(profiles);
            String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);

            NewCookie cookie = new NewCookie(cookieName, encoded, cookiePath, null, cookieComment, maxAge, httpOnly);
            log.infof("Setting cookie '%s' for user '%s'", cookieName, user.getUsername());

            return Response.fromResponse(formResponse).cookie(cookie).build();
        } catch (Exception e) {
            log.error("Failed to serialize or encode user profiles", e);
            return formResponse;
        }
    }

    private List<Map<String, String>> loadExistingProfiles(AuthenticationFlowContext context, String cookieName) {
        try {
            String rawValue = context.getHttpRequest().getHttpHeaders()
                    .getCookies()
                    .getOrDefault(cookieName, null)
                    .getValue();

            if (StringUtil.isBlank(rawValue)) return new ArrayList<>();

            String decoded = java.net.URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(decoded, List.class);
        } catch (Exception e) {
            log.warnf("Failed to read or parse cookie '%s': %s", cookieName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void addUserProfileIfAbsent(List<Map<String, String>> profiles, UserModel user) {
        String email = user.getEmail();
        boolean exists = profiles.stream().anyMatch(p -> email.equalsIgnoreCase(p.get("email")));

        if (!exists) {
            Map<String, String> profile = new HashMap<>();
            profile.put("name", user.getFirstName() + " " + user.getLastName());
            profile.put("email", email);
            profiles.add(profile);
            log.infof("Added profile for: %s", email);
        } else {
            log.debugf("Profile for '%s' already exists. Skipping add.", email);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {
        return user.getEmail() != null;
    }

    @Override
    public void setRequiredActions(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {}

    @Override
    public void close() {}
}
