package com.keycloak.otp.auth.browser;

import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.WebAuthnConditionalUIAuthenticator;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.services.validation.Validation.FIELD_PASSWORD;
import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

public class CustomUsernamePasswordForm extends AbstractUsernameFormAuthenticator implements Authenticator {

    public static final Logger LOG = Logger.getLogger(CustomUsernamePasswordForm.class);

    protected final WebAuthnConditionalUIAuthenticator webauthnAuth;

    public CustomUsernamePasswordForm() {
        webauthnAuth = null;
    }

    public CustomUsernamePasswordForm(KeycloakSession session) {
        webauthnAuth = new WebAuthnConditionalUIAuthenticator(session, (context) -> createLoginForm(context.form()));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        LOG.infof("Form data submitted: %s", formData);
        Utils.setClientIpInAuthSession(formData.getFirst(Constants.CLIENT_IP), context.getAuthenticationSession());

        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        } else if (webauthnAuth != null && webauthnAuth.isPasskeysEnabled()
                && (formData.containsKey(WebAuthnConstants.AUTHENTICATOR_DATA) || formData.containsKey(WebAuthnConstants.ERROR))) {
            // webauth form submission, try to action using the webauthn authenticator
            webauthnAuth.action(context);
            return;
        } else if (!validateForm(context, formData)) {
            // normal username and form authenticator
            return;
        }
        context.success(PasswordCredentialModel.TYPE);
    }

    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return validateUserAndPassword(context, formData);
    }

    protected boolean alreadyAuthenticatedUsingPasswordlessCredential(AuthenticationFlowContext context) {
        return alreadyAuthenticatedUsingPasswordlessCredential(context.getAuthenticationSession());
    }

    /**
     * Keycloak 26.x safe: don't call getCredentialType() (can be protected depending on class),
     * compare the auth note against WebAuthnCredentialModel.TYPE_PASSWORDLESS.
     */
    protected boolean alreadyAuthenticatedUsingPasswordlessCredential(AuthenticationSessionModel authSession) {
        return webauthnAuth != null
                && webauthnAuth.isPasskeysEnabled()
                && WebAuthnCredentialModel.TYPE_PASSWORDLESS.equals(
                authSession.getAuthNote(AuthenticationProcessor.LAST_AUTHN_CREDENTIAL)
        );
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        String loginHint = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        String rememberMeUsername = AuthenticationManager.getRememberMeUsername(context.getSession());

        if (context.getUser() != null) {
            if (alreadyAuthenticatedUsingPasswordlessCredential(context)) {
                // if already authenticated using passwordless webauthn just success
                context.success();
                return;
            }

            LoginFormsProvider form = context.form();
            form.setAttribute(LoginFormsProvider.USERNAME_HIDDEN, true);
            form.setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);
            context.getAuthenticationSession().setAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH, "true");
        } else {
            context.getAuthenticationSession().removeAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH);
            if (loginHint != null || rememberMeUsername != null) {
                if (loginHint != null) {
                    formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
                } else {
                    formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                    formData.add("rememberMe", "on");
                }
            }
            // setup webauthn data when the user is not already selected
            if (webauthnAuth != null && webauthnAuth.isPasskeysEnabled()) {
                webauthnAuth.fillContextForm(context);
            }
        }
        Response challengeResponse = challenge(context, formData);
        context.challenge(challengeResponse);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.form();

        if (!formData.isEmpty()) forms.setFormData(formData);

        return forms.createLoginUsernamePassword();
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        if (context.getUser() == null && webauthnAuth != null && webauthnAuth.isPasskeysEnabled()) {
            // setup webauthn data when the user is not already selected
            webauthnAuth.fillContextForm(context);
        }
        return super.challenge(context, error, field);
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // never called
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // never called
    }

    @Override
    public void close() {

    }

    @Override
    public void testInvalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
            AuthenticatorUtils.dummyHash(context);
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
        }
    }

    @Override
    public boolean enabledUser(AuthenticationFlowContext context, UserModel user) {
        if (isDisabledByBruteForce(context, user)) return false;
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(Errors.USER_DISABLED);
            Response challengeResponse = challenge(context, Messages.ACCOUNT_DISABLED);
            context.forceChallenge(challengeResponse);
            return false;
        }
        return true;
    }

    @Override
    protected boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        String bruteForceError = getDisabledByBruteForceEventError(context, user);
        if (bruteForceError != null) {
            context.getEvent().user(user);
            context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(bruteForceError);
            Response challengeResponse = challenge(context, disabledByBruteForceError(bruteForceError), disabledByBruteForceFieldError());
            context.forceChallenge(challengeResponse);
            return true;
        }
        return false;
    }

    @Override
    public boolean validatePassword(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData, boolean clearUser) {
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            return badPasswordHandler(context, user, clearUser,true);
        }

        if (isDisabledByBruteForce(context, user)) return false;

        if (user.credentialManager().isValid(UserCredentialModel.password(password))) {
            context.getAuthenticationSession().setAuthNote(AuthenticationManager.PASSWORD_VALIDATED, "true");
            return true;
        } else {
            return badPasswordHandler(context, user, clearUser,false);
        }
    }

    // Set up AuthenticationFlowContext error.
    private boolean badPasswordHandler(AuthenticationFlowContext context, UserModel user, boolean clearUser,boolean isEmptyPassword) {
        context.getEvent().user(user);
        context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).error(Errors.INVALID_USER_CREDENTIALS);

        if (isUserAlreadySetBeforeUsernamePasswordAuth(context)) {
            LoginFormsProvider form = context.form();
            form.setAttribute(LoginFormsProvider.USERNAME_HIDDEN, true);
            form.setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);
        }

        Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_PASSWORD);
        if(isEmptyPassword) {
            context.forceChallenge(challengeResponse);
        }else{
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }

        if (clearUser) {
            context.clearUser();
        }
        return false;
    }

}
