package com.keycloak.otp.auth.forms;

import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.userprofile.*;
import org.keycloak.util.JsonSerialization;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RegistrationUserCreationCapture implements FormAction, FormActionFactory {
    private static final Logger LOG = Logger.getLogger(RegistrationUserCreationCapture.class);


    public static final String PROVIDER_ID = "reg-user-creation-capture";

    @Override
    public String getHelpText() {
        return "Validates profile and stores registration fields in session notes; does not create the user.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> props = new ArrayList<>();

        ProviderConfigProperty captureParams = new ProviderConfigProperty();
        captureParams.setName(Constants.CAPTURE_QUERY_PARAMS);
        captureParams.setLabel("Capture Query Params");
        captureParams.setHelpText("Comma-separated list of query parameter names to capture into auth notes.");
        captureParams.setType(ProviderConfigProperty.STRING_TYPE);

        props.add(captureParams);
        return props;
    }

    @Override
    public void validate(ValidationContext context) {
        LOG.infof("RegistrationUserCreationCapture.validate called for realm %s", context.getRealm().getName());

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        LOG.infof("Form data received: %s", formData);
        Utils.setClientIpInAuthSession(formData.getFirst(Constants.CLIENT_IP), context.getAuthenticationSession());

        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        UserProfile profile = getOrCreateUserProfile(context, formData);
        Attributes attributes = profile.getAttributes();
        String email = attributes.getFirst(UserModel.EMAIL);

        if (!validateOrganizationInvitation(context, formData, email)) {
            return;
        }

        String username = attributes.getFirst(UserModel.USERNAME);
        String firstName = attributes.getFirst(UserModel.FIRST_NAME);
        String lastName = attributes.getFirst(UserModel.LAST_NAME);

        context.getEvent().detail(Details.EMAIL, email);
        context.getEvent().detail(Details.USERNAME, username);
        context.getEvent().detail(Details.FIRST_NAME, firstName);
        context.getEvent().detail(Details.LAST_NAME, lastName);
        context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession()));

        if (context.getRealm().isRegistrationEmailAsUsername()) {
            context.getEvent().detail(Details.USERNAME, email);
        }

        try {
            profile.validate();
        } catch (ValidationException pve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());

            if (pve.hasError(Messages.EMAIL_EXISTS, Messages.INVALID_EMAIL)) {
                context.getEvent().detail(Details.EMAIL, attributes.getFirst(UserModel.EMAIL));
            }

            if (pve.hasError(Messages.EMAIL_EXISTS)) {
                context.error(Errors.EMAIL_IN_USE);
            } else if (pve.hasError(Messages.USERNAME_EXISTS)) {
                context.error(Errors.USERNAME_IN_USE);
            } else {
                context.error(Errors.INVALID_REGISTRATION);
            }

            context.validationError(formData, errors);
            return;
        }
        LOG.infof("RegistrationUserCreationCapture.validate succeeded for username %s", username);
        context.success();
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        captureProfileParam(context);
        checkNotOtherUserAuthenticating(context);
    }

    @Override
    public void success(FormContext context) {
        LOG.infof("RegistrationUserCreationCapture.success called for realm %s", context.getRealm().getName());
        checkNotOtherUserAuthenticating(context);

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String email = formData.getFirst(UserModel.EMAIL);
        String username = formData.getFirst(UserModel.USERNAME);
        if (context.getRealm().isRegistrationEmailAsUsername()) username = email;

        context.getEvent().detail(Details.USERNAME, username)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.EMAIL, email);

        // Keep a validated profile for later creation
        UserProfile profile = getOrCreateUserProfile(context, formData);

        // Store fields in auth notes for downstream steps
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        auth.setAuthNote(Constants.NOTE_USERNAME, profile.getAttributes().getFirst(UserModel.USERNAME));
        auth.setAuthNote(Constants.NOTE_EMAIL, profile.getAttributes().getFirst(UserModel.EMAIL));
        auth.setAuthNote(Constants.NOTE_FIRST, profile.getAttributes().getFirst(UserModel.FIRST_NAME));
        auth.setAuthNote(Constants.NOTE_LAST, profile.getAttributes().getFirst(UserModel.LAST_NAME));

        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(auth, "capture user", "registration");

        // Terms checkbox capture (do not mutate user here)
        if ("on".equals(formData.getFirst("termsAccepted"))) {
            auth.setAuthNote(Constants.NOTE_TAC, "on");
        }

        captureCustomFormAttributesToNotes(context, formData);

        // Hint for next steps
        auth.setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, username);

        // Keep profile in session for the final creator
        context.getSession().setAttribute("UP_REGISTER", profile);
        LOG.infof("Stored user profile in session for username %s", username);
        // DO NOT create user and DO NOT set context.setUser(user) here.
//        context.success();
    }

    private void captureProfileParam(FormContext context) {
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        LOG.info("setting APP_INITIATED_FLOW note to " + LoginActionsService.REGISTRATION_PATH);
        auth.setClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW, LoginActionsService.REGISTRATION_PATH);

        String configured = Utils.getConfigString(context.getAuthenticatorConfig(), Constants.CAPTURE_QUERY_PARAMS, null);

        if (configured == null || configured.isBlank()) {
            LOG.info("No captureQueryParams configured");
            return;
        }

        Set<String> names = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        MultivaluedMap<String, String> qp = context.getHttpRequest().getUri().getQueryParameters();
        if (qp == null) return;

        for (String name : names) {
            String val = qp.getFirst(name);
            if (val != null && !val.isBlank()) {
                String noteKey = Constants.QP_PREPEND + name;
                auth.setAuthNote(noteKey, val);
                LOG.infof("Captured query param %s=%s into authNote %s", name, val, noteKey);
            }
        }
    }

    private void captureCustomFormAttributesToNotes(FormContext context, MultivaluedMap<String, String> formData) {
        Map<String, java.util.List<String>> extras = new LinkedHashMap<>();

        for (String key : formData.keySet()) {
            if (!key.startsWith("user.attributes.")) continue;
            String attr = key.substring("user.attributes.".length());
            if (attr.isBlank()) continue;

            java.util.List<String> vals = formData.get(key);
            if (vals == null || vals.isEmpty()) continue;

            // copy to avoid underlying map being mutable
            extras.put(attr, new java.util.ArrayList<>(vals));
        }

        try {
            AuthenticationSessionModel auth = context.getAuthenticationSession();
            if (extras.isEmpty()) {
                auth.removeAuthNote(Constants.NOTE_ATTRS_JSON);
                LOG.infof("No custom user.attributes.* found to capture.");
            } else {
                String json = JsonSerialization.writeValueAsString(extras);
                auth.setAuthNote(Constants.NOTE_ATTRS_JSON, json);
                LOG.infof("Captured custom attrs into %s: %s", Constants.NOTE_ATTRS_JSON, json);
            }
        } catch (Exception e) {
            LOG.warn("Failed to serialize custom attributes to reg.attrs.json", e);
        }
    }


    private void checkNotOtherUserAuthenticating(FormContext context) {
        if (context.getUser() != null) {
            // the user probably did some back navigation in the browser, hitting this page in a strange state
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).detail(Details.EXISTING_USER, context.getUser().getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, Errors.DIFFERENT_USER_AUTHENTICATING, Messages.EXPIRED_ACTION);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }


    @Override
    public void close() {

    }

    @Override
    public String getDisplayType() {
        return "Registration User Profile (capture only)";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public FormAction create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private MultivaluedMap<String, String> normalizeFormParameters(MultivaluedMap<String, String> formParams) {
        MultivaluedHashMap<String, String> copy = new MultivaluedHashMap<>(formParams);

        // Remove google recaptcha form property to avoid length errors
//        copy.remove(RegistrationPage.FIELD_RECAPTCHA_RESPONSE);  //commented out as this is not available on our production keycloak version 24.0.5
        // Remove "password" and "password-confirm" to avoid leaking them in the user-profile data
        copy.remove(RegistrationPage.FIELD_PASSWORD);
        copy.remove(RegistrationPage.FIELD_PASSWORD_CONFIRM);

        return copy;
    }

    /**
     * Get user profile instance for current HTTP request (KeycloakSession) and for given context. This assumes that there is
     * single user registered within HTTP request, which is always the case in Keycloak
     */
    public UserProfile getOrCreateUserProfile(FormContext formContext, MultivaluedMap<String, String> formData) {
        KeycloakSession session = formContext.getSession();
        UserProfile profile = (UserProfile) session.getAttribute("UP_REGISTER");
        if (profile == null) {
            formData = normalizeFormParameters(formData);
            UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
            profile = profileProvider.create(UserProfileContext.REGISTRATION, formData);
            session.setAttribute("UP_REGISTER", profile);
        }
        return profile;
    }

    private boolean validateOrganizationInvitation(ValidationContext context, MultivaluedMap<String, String> formData, String email) {
        //commenting out below org invitation validation code as this is not available on our production keycloak version 24.0.5.
        if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            Consumer<List<FormMessage>> error = messages -> {
                context.error(Errors.INVALID_TOKEN);
                context.validationError(formData, messages);
            };

            InviteOrgActionToken token;

            try {
                token = Organizations.parseInvitationToken(context.getHttpRequest());
            } catch (VerificationException e) {
                error.accept(List.of(new FormMessage("Unexpected error parsing the invitation token")));
                return false;
            }

            if (token == null) {
                return true;
            }

            KeycloakSession session = context.getSession();
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = provider.getById(token.getOrgId());

            if (organization == null) {
                error.accept(List.of(new FormMessage("The provided token contains an invalid organization id")));
                return false;
            }

            // make sure the organization is set to the session so that UP org-related validators can run
            session.getContext().setOrganization(organization);
            session.setAttribute(InviteOrgActionToken.class.getName(), token);

            if (token.isExpired() || !token.getActionId().equals(InviteOrgActionToken.TOKEN_TYPE)) {
                error.accept(List.of(new FormMessage("The provided token is not valid or has expired.")));
                return false;
            }

            if (!token.getEmail().equals(email)) {
                error.accept(List.of(new FormMessage(UserModel.EMAIL, "Email does not match the invitation")));
                return false;
            }
        }

        return true;
    }

    private void addOrganizationMember(FormContext context, UserModel user) {
        if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            InviteOrgActionToken token = (InviteOrgActionToken) context.getSession().getAttribute(InviteOrgActionToken.class.getName());

            if (token != null) {
                KeycloakSession session = context.getSession();
                OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
                OrganizationModel orgModel = provider.getById(token.getOrgId());
                provider.addManagedMember(orgModel, user);
                context.getEvent().detail(Details.ORG_ID, orgModel.getId());
                context.getAuthenticationSession().setRedirectUri(token.getRedirectUri());
            }
        }
    }
}
