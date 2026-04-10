/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keycloak.otp.auth.browser;

import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.WebAuthnConditionalUIAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;


public class UsernamePhonePasswordForm extends AbstractUsernameFormAuthenticator implements Authenticator {
    private static final Logger LOG = Logger.getLogger(UsernamePhonePasswordForm.class);

    protected final WebAuthnConditionalUIAuthenticator webauthnAuth;

    public UsernamePhonePasswordForm() {
        webauthnAuth = null;
    }

    public UsernamePhonePasswordForm(KeycloakSession session) {
        webauthnAuth = new WebAuthnConditionalUIAuthenticator(session, (context) -> createLoginForm(context.form()));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        LOG.info("UsernamePhonePasswordForm action called with form data: " + formData);
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
        LOG.info("Validating form data: " + formData);

        String identifier = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (identifier == null) {
            return validateUserAndPassword(context, formData);
        }

        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        UserModel user = null;

        // 1) Username lookup (fast)
        user = session.users().getUserByUsername(realm, identifier);

        // 2) Email lookup (fast) - only if it looks like email
        if (user == null && Utils.isEmail(identifier)) {
            LOG.info("Looking up user by email: " + identifier);
            user = session.users().getUserByEmail(realm, identifier);
        }

        if (user == null) {
            String phone = Utils.normalizePhone(identifier);
            LOG.info("Looking up user by phone number: " + phone);
            user = Utils.findUserByPhone(context, phone);
            LOG.info("User lookup by phone number '" + phone + "' returned: " + (user != null ? user.getUsername() : "null"));
        }

        if (user != null) {
            context.setUser(user);
            context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.USER_SET_BEFORE_USERNAME_PASSWORD_AUTH, "true");
            context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, identifier);
        } else {
            context.getAuthenticationSession().removeAuthNote(AbstractUsernameFormAuthenticator.USER_SET_BEFORE_USERNAME_PASSWORD_AUTH);
            context.clearUser();
        }

        boolean result = validateUserAndPassword(context, formData);
        LOG.info("Username/password validation with phone number succeeded: " + result);
        return result;
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
        LOG.info("UsernamePhonePasswordForm authenticate called");
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

}
