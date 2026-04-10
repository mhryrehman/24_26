package com.keycloak.otp.auth.forms;

import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import com.keycloak.otp.util.Constants;

import java.util.ArrayList;
import java.util.List;


public class RegistrationPasswordCapture implements FormAction, FormActionFactory {
    private static final Logger LOG = Logger.getLogger(RegistrationPasswordCapture.class);

    public static final String PROVIDER_ID = "reg-password-action-capture";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_CONFIRM  = "password-confirm";

    @Override
    public String getHelpText() {
        return "Validates password and confirmation against policy; stores it to be set later (after OTP).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void validate(ValidationContext context) {
        LOG.infof("RegistrationPasswordCapture.validate");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();

        context.getEvent().detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(context.getAuthenticationSession())).detail(Details.REGISTER_METHOD, "form");

        if (Validation.isBlank(formData.getFirst(RegistrationPage.FIELD_PASSWORD))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, Messages.MISSING_PASSWORD));
        } else if (!formData.getFirst(RegistrationPage.FIELD_PASSWORD).equals(formData.getFirst(RegistrationPage.FIELD_PASSWORD_CONFIRM))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD_CONFIRM, Messages.INVALID_PASSWORD_CONFIRM));
        }
        if (formData.getFirst(RegistrationPage.FIELD_PASSWORD) != null) {
            PolicyError err = context.getSession().getProvider(PasswordPolicyManagerProvider.class).validate(context.getRealm().isRegistrationEmailAsUsername() ? formData.getFirst(RegistrationPage.FIELD_EMAIL) : formData.getFirst(RegistrationPage.FIELD_USERNAME), formData.getFirst(RegistrationPage.FIELD_PASSWORD));
            if (err != null)
                errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, err.getMessage(), err.getParameters()));
        }

        if (errors.size() > 0) {
            context.error(Errors.INVALID_REGISTRATION);
            formData.remove(RegistrationPage.FIELD_PASSWORD);
            formData.remove(RegistrationPage.FIELD_PASSWORD_CONFIRM);
            context.validationError(formData, errors);
            return;
        } else {
            context.success();
        }
    }

    @Override
    public void success(FormContext context) {
        LOG.infof("RegistrationPasswordCapture.success");
        String pw = context.getHttpRequest().getDecodedFormParameters().getFirst(FIELD_PASSWORD);
        context.getAuthenticationSession().setAuthNote(Constants.NOTE_PASSWORD, pw);
    }


    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        form.setAttribute("passwordRequired", true);
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
        return "Password Validation capture";
    }

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return false;
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
}
