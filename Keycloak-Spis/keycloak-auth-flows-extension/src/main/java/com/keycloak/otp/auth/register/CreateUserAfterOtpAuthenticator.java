package com.keycloak.otp.auth.register;

import com.fasterxml.jackson.core.type.TypeReference;
import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Map;

public class CreateUserAfterOtpAuthenticator implements Authenticator, AuthenticatorFactory {
    private static final Logger LOG = Logger.getLogger(CreateUserAfterOtpAuthenticator.class);

    public static final String PROVIDER_ID = "create-user-after-otp";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.infof("CreateUserAfterOtpAuthenticator.authenticate called");

        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.USER_CREATED, context.getFlowPath());

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        LoginFormsProvider form = context.form();

        String username = auth.getAuthNote(Constants.NOTE_USERNAME);
        String email = auth.getAuthNote(Constants.NOTE_EMAIL);
        String first = auth.getAuthNote(Constants.NOTE_FIRST);
        String last = auth.getAuthNote(Constants.NOTE_LAST);
        String tac = auth.getAuthNote(Constants.NOTE_TAC);

        LOG.infof("Captured notes: username=%s email=%s first=%s last=%s tac=%s", username, email, first, last, tac);

        // Password captured by RegistrationPasswordCapture
        String plainPassword = auth.getAuthNote(Constants.NOTE_PASSWORD);
        // Basic presence check (should already be validated earlier)
        if (username == null || email == null || plainPassword == null) {
            LOG.infof("Missing required registration data, cannot create user");
            context.getEvent().detail(Constants.FAILURE_DETAILS, Constants.INVALID_REGISTRATION_DATA).detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(Errors.INVALID_REGISTRATION);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        // Uniqueness defensive check (race conditions)
        if (session.users().getUserByUsername(realm, username) != null) {
            LOG.infof("Username already exists: %s", username);
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).detail(Constants.FAILURE_DETAILS, Constants.USERNAME_ALREADY_EXISTS).error(Errors.INVALID_REGISTRATION);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, form.setError(Messages.USERNAME_EXISTS).createErrorPage(Response.Status.CONFLICT));
            return;
        }
        if (!realm.isDuplicateEmailsAllowed() && session.users().getUserByEmail(realm, email) != null) {
            LOG.infof("Email already exists: %s", email);
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).detail(Constants.FAILURE_DETAILS, Constants.EMAIL_ALREADY_EXISTS).error(Errors.INVALID_REGISTRATION);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, form.setError(Messages.EMAIL_EXISTS).createErrorPage(Response.Status.CONFLICT));
            return;
        }

        UserModel user;
        user = session.users().addUser(realm, KeycloakModelUtils.generateId(), username, true, false);
        user.setEnabled(true);
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEnabled(true);
        user.setEmailVerified(true);
        applyCapturedAttributesFromJson(context, user);

        // Apply password
        try {
            LOG.infof("kcId=%s Setting user password", user.getId());
            user.credentialManager().updateCredential(UserCredentialModel.password(plainPassword, false));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to set password for user %s: %s", username, e.getMessage());
                context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).detail(Constants.FAILURE_DETAILS, Constants.FAILED_TO_SET_PASSWORD).error(Errors.INVALID_REGISTRATION);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }

        // Handle Terms & Conditions note captured earlier
        if ("on".equals(tac)) {
            RequiredActionProviderModel tacModel = realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
            if (tacModel != null && tacModel.isEnabled()) {
                user.setSingleAttribute(TermsAndConditions.USER_ATTRIBUTE, Integer.toString(Time.currentTime()));
                auth.removeRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
                user.removeRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
            }
        }

        //commenting out below code as this is not available on our production keycloak version 24.0.5.
        // If Organizations feature is on and invite token present, add membership (mirrors stock behavior)
        if (Profile.isFeatureEnabled(org.keycloak.common.Profile.Feature.ORGANIZATION)) {
            InviteOrgActionToken token = (InviteOrgActionToken) session.getAttribute(InviteOrgActionToken.class.getName());
            if (token != null) {
                var provider = session.getProvider(org.keycloak.organization.OrganizationProvider.class);
                var orgModel = provider.getById(token.getOrgId());
                if (orgModel != null) {
                    provider.addManagedMember(orgModel, user);
                    context.getEvent().detail(Details.ORG_ID, orgModel.getId());
                    auth.setRedirectUri(token.getRedirectUri());
                }
            }
        }

        // Event details (like stock)
        context.getEvent().user(user);
        context.getEvent().detail(Details.USERNAME, username);
        context.getEvent().detail(Details.EMAIL, email);
        context.getEvent().event(EventType.REGISTER).success();
        context.getEvent().success();
        context.newEvent().event(EventType.LOGIN);
        context.getEvent().client(auth.getClient().getClientId()).detail(Details.REDIRECT_URI, auth.getRedirectUri()).detail(Details.AUTH_METHOD, auth.getProtocol());
        String authType = auth.getAuthNote(Details.AUTH_TYPE);
        if (authType != null) context.getEvent().detail(Details.AUTH_TYPE, authType);

        try {
            // Register user in Leap system
            LeapIntegrationService leap = new LeapIntegrationService(context.getSession());
            Map<String, Object> result = leap.registerUserInLeap(user, context.getAuthenticationSession());
            applyLeapAttributesToUser(user, result);

        } catch (Exception e) {
            LOG.errorf(e, "Leap registration failed; rolling back Keycloak user %s", user.getUsername());
            boolean removed = context.getSession().users().removeUser(context.getRealm(), user);
            LOG.warnf("Rollback remove user %s -> %s", user.getUsername(), removed);
            context.getEvent().detail(Constants.FAILURE_DETAILS, Constants.LEAP_REGISTRATION_FAILED).detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(Errors.INVALID_REGISTRATION);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    form.setError(Constants.REGISTRATION_EXTERNAL_ERROR)
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }


        // Bind user to context and advance
        context.setUser(user);
        auth.setAuthenticatedUser(user);
        clearSensitiveNotes(context);

        context.success();
    }

    private void applyCapturedAttributesFromJson(AuthenticationFlowContext context, UserModel user) {
        String json = context.getAuthenticationSession().getAuthNote(Constants.NOTE_ATTRS_JSON);
        LOG.infof("kcId=%s Applying extra attributes from JSON: " + json, user.getId());
        if (json == null || json.isBlank()) return;

        try {
            Map<String, List<String>> extras = JsonSerialization.readValue(json, new TypeReference<>() {
            });
            if (extras == null || extras.isEmpty()) return;

            for (Map.Entry<String, List<String>> e : extras.entrySet()) {
                List<String> vals = e.getValue();
                if (vals == null || vals.isEmpty()) continue;

                if (vals.size() == 1) user.setSingleAttribute(e.getKey(), vals.get(0));
                else user.setAttribute(e.getKey(), vals);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to parse/apply reg.attrs.json", ex);
        }
    }


    private void clearSensitiveNotes(AuthenticationFlowContext context) {
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        // scrub password and otp-related notes; leave non-sensitive hints if you want
        auth.removeAuthNote(Constants.NOTE_PASSWORD);
        auth.removeAuthNote("otp");            // if you used this key
        auth.removeAuthNote("otp.ttl");        // if you used this key
        auth.removeAuthNote(Constants.NOTE_USERNAME);
        auth.removeAuthNote(Constants.NOTE_EMAIL);
        auth.removeAuthNote(Constants.NOTE_FIRST);
        auth.removeAuthNote(Constants.NOTE_LAST);
        auth.removeAuthNote(Constants.NOTE_TAC);
        // Remove cached UserProfile
        context.getSession().removeAttribute("UP_REGISTER");
    }

    /* ---------- Authenticator interface (no UI action needed here) ---------- */

    @Override
    public void action(AuthenticationFlowContext context) { /* not used */ }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {
    }

    @Override
    public void close() {
    }

    /* -------------------- Factory boilerplate -------------------- */

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getDisplayType() {
        return "Create User (after OTP)";
    }

    @Override
    public String getReferenceCategory() {
        return "registration";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED, AuthenticationExecutionModel.Requirement.ALTERNATIVE};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Creates the user from captured registration data after OTP verification.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    private void applyLeapAttributesToUser(UserModel user, Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            LOG.warnf("kcId=%s Leap response empty; skipping attribute mapping", user.getId());
            throw new RuntimeException("Leap response empty");
        }

        // Strings
        String firstName = Utils.getStringFromObject(m.get("first_name"));
        String lastName = Utils.getStringFromObject(m.get("last_name"));
        String email = Utils.getStringFromObject(m.get("email"));
        String gender = Utils.getStringFromObject(m.get("gender"));
        String mobile = Utils.getStringFromObject(m.get("mobile_number"));
        String userType = Utils.getStringFromObject(m.get("user_type"));
        String leapId = Utils.getStringFromObject(m.get("leap_id"));
        String emailVerifiedString = Utils.getStringFromObject(m.get("email_verified"));

        // Booleans
        Boolean mobileVerified = Utils.getBooleanFromObject(m.get("mobile_number_verified"));
        Boolean emailVerified = Utils.getBooleanFromObject(m.get("email_verified"));

        // Lists
        List<String> userRoles = Utils.getListFromObject(m.get("user_roles"));

        // --- Map to Keycloak core fields ---
//        if (!StringUtil.isBlank(firstName)) user.setFirstName(firstName);
//        if (!StringUtil.isBlank(lastName))  user.setLastName(lastName);
//        if (!StringUtil.isBlank(email))     user.setEmail(email);
        if (emailVerified != null) user.setEmailVerified(emailVerified);

        // --- Map to user attributes (stringify booleans for KC attributes) ---
        if (!StringUtil.isBlank(emailVerifiedString))    user.setSingleAttribute("email_verified", emailVerifiedString);
        if (!StringUtil.isBlank(gender))    user.setSingleAttribute("gender", gender);
        if (!StringUtil.isBlank(mobile)) user.setSingleAttribute("mobile_number", mobile);
        if (mobileVerified != null) user.setSingleAttribute("mobile_number_verified", mobileVerified.toString());
        if (!StringUtil.isBlank(userType)) user.setSingleAttribute("user_type", userType);
        if (!StringUtil.isBlank(leapId)) user.setSingleAttribute("leap_id", leapId);
        if (userRoles != null && !userRoles.isEmpty()) user.setAttribute("user_roles", userRoles);

        LOG.infof("kcId=%s Applied Leap attributes to user %s", user.getId(), user.getUsername());
    }
}

