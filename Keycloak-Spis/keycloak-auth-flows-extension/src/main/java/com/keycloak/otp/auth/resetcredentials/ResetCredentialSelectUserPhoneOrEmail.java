package com.keycloak.otp.auth.resetcredentials;

import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import java.util.List;

public class ResetCredentialSelectUserPhoneOrEmail implements Authenticator, AuthenticatorFactory {

    private static final Logger logger = Logger.getLogger(org.keycloak.authentication.authenticators.resetcred.ResetCredentialChooseUser.class);

    public static final String PROVIDER_ID = "validate-user-exists-phone";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String existingUserId = context.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.EXISTING_USER_INFO);
        if (existingUserId != null) {
            UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());

            logger.infof("kcId=%s Forget-password(phone or email) triggered when authenticating user after first broker login. Prefilling reset-credential-choose-user screen with user '%s' ", existingUser.getId(), existingUser.getUsername());
            context.setUser(existingUser);
            Response challenge = context.form().createPasswordReset();
            context.challenge(challenge);
            return;
        }

        String actionTokenUserId = context.getAuthenticationSession().getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
        if (actionTokenUserId != null) {
            UserModel existingUser = context.getSession().users().getUserById(context.getRealm(), actionTokenUserId);

            // Action token logics handles checks for user ID validity and user being enabled

            logger.infof("kcId=%s Forget-password triggered when reauthenticating user after authentication via action token. Skipping reset-credential-choose-user screen and using user '%s' ", existingUser.getId(), existingUser.getUsername());
            context.setUser(existingUser);
            context.success();
            return;
        }

        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(), context.getRealm(), true);
        //skip user choice if sso session exists
        if (authResult != null) {
            logger.infof("kcId=%s Existing SSO session found, skipping user selection for reset-credential flow for user: " + authResult.getUser().getUsername(), authResult.getUser().getId());
            context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, authResult.getUser().getUsername());
            context.setUser(authResult.getUser());
            context.success();
            return;
        }

        Response challenge = context.form().createPasswordReset();
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = formData.getFirst("username");
        logger.infof("ResetCredentialSelectUserPhoneOrEmail action for username: %s", username);
        if (username == null || username.isEmpty()) {
            logger.info("Username not provided");
            event.error(Errors.USERNAME_MISSING);
            Response challenge = context.form()
                    .addError(new FormMessage(Validation.FIELD_USERNAME, Messages.MISSING_USERNAME))
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        username = username.trim();

        //setting username in auth note, so it can be used in UserActivityLoggerService.
        context.getAuthenticationSession().setAuthNote(Constants.NOTE_EMAIL, username);
        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.USER_SELECTED, context.getFlowPath());

        RealmModel realm = context.getRealm();
        UserModel user = context.getSession().users().getUserByUsername(realm, username);
        if (user == null && realm.isLoginWithEmailAllowed() && username.contains("@")) {
            logger.info("Looking up user by email: " + username);
            user = context.getSession().users().getUserByEmail(realm, username);
        }

        context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        if (user == null && Utils.isPhone(username)) {
            // try to lookup by phone number
            logger.info("Looking up user by phone: " + username);
            user = Utils.findUserByPhone(context, username);
            logger.info("User found by phone: " + (user != null ? user.getUsername() : "null"));
        }

        // we don't want people guessing usernames, so if there is a problem, just continue, but don't set the user
        // a null user will notify further executions, that this was a failure.
        if (user == null) {
            logger.info("User not found: " + username);
            event.clone().detail(Details.USERNAME, username).error(Errors.USER_NOT_FOUND);
            context.clearUser();

            event.error(Errors.USERNAME_MISSING);
            Response challenge = context.form()
                    .setAttribute(Constants.AUTH_ERROR_KEY, Constants.EMAIL_INVALID)
                    .addError(new FormMessage(Validation.FIELD_USERNAME, Constants.EMAIL_INVALID))
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;

        } else if (!user.isEnabled()) {
            logger.info("Account is disabled for user: " + username);
            event.clone().detail(Details.USERNAME, username).user(user).error(Errors.USER_DISABLED);
            context.clearUser();
            event.error(Errors.USER_DISABLED);

            Response challenge = context.form()
                    .setAttribute(Constants.AUTH_ERROR_KEY, Constants.ACCOUNT_DISABLED_CONTACT_ADMIN)
                    .addError(new FormMessage(Validation.FIELD_USERNAME, Constants.ACCOUNT_DISABLED_CONTACT_ADMIN))
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        } else {
            context.setUser(user);
        }

        context.success();
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
    public String getDisplayType() {
        return "Choose User by Username, Email, or Phone (with user validation)";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Choose a user by Username, Email, or PhoneNumber during authentication. It will return error if user did not exist in keycloak.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
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
}

