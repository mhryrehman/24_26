package com.keycloak.otp.auth.browser;

import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.service.NotificationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Yasir Rehman
 * <p>
 * This class is a form-based authenticator used in browser-based login flows.
 * It sends an OTP code to the user's email and validates it against the form input.
 */
@JBossLog
public class OtpAuthenticatorForm extends AbstractUsernameFormAuthenticator {

    private static final Logger log = Logger.getLogger(OtpAuthenticatorForm.class);

    /**
     * Starts the authentication process. If OTP is enabled, triggers challenge.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!"reset-credentials".equals(context.getFlowPath())) {
            LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
            uals.logActivity(context.getAuthenticationSession(), Constants.LOGIN_FORM_SUBMITED, context.getFlowPath());
        }

        UserModel user = context.getUser();
        String username = user.getUsername();
        log.infof("kcId=%s EmailAuthenticator invoked", user.getId());

        boolean isEmailOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED));
        boolean isSmsOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_SMS_ENABLED));
        boolean isAlwaysExecute = Utils.getConfigBool(context.getAuthenticatorConfig(), Constants.ALWAYS_EXECUTE, false);

        if (!isAlwaysExecute && !isEmailOtpEnabled && !isSmsOtpEnabled) {
            log.infof("kcId=%s Skipping OTP for user %s (OTP not enabled via email or SMS)", user.getId(), username);
            context.success();
            return;
        }

        if (!Utils.isUserLocked(context, user) && context.getAuthenticationSession().getAuthNote(Constants.CODE) == null) {
            generateAndSendEmailCode(context);
        }

        Response challengeResponse = renderOtpForm(context, null, null);
        context.challenge(challengeResponse);
    }

    private Response renderOtpForm(AuthenticationFlowContext context, String error, String field) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        LoginFormsProvider form;
        UserModel user = context.getUser();
        log.infof("kcId=%s Rendering OTP form.", user.getId());
        boolean isEmailOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED));
        boolean isSmsOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_SMS_ENABLED));
        boolean isAlwaysExecute = Utils.getConfigBool(context.getAuthenticatorConfig(), Constants.ALWAYS_EXECUTE, false);

        String email = user.getEmail();
        String mobileNumber = user.getFirstAttribute(Constants.MOBILE_NUMBER);

        Map<String, Object> attributes = new HashMap<>();

        int expirySeconds = Utils.getConfigInt(config, Constants.CODE_TTL, Constants.DEFAULT_TTL);
        int codeLength = Utils.getConfigInt(config, Constants.CODE_LENGTH, Constants.DEFAULT_LENGTH);
        int resendTime = Utils.getConfigInt(config, Constants.RESEND_TIME, Constants.DEFAULT_RESEND_TIME);

        String ttlNote = context.getAuthenticationSession().getAuthNote(Constants.CODE_TTL);
        String otpJustSent = context.getAuthenticationSession().getAuthNote(Constants.OTP_SENT_FLAG);
        log.infof("kcId=%s OTP form attributes: expirySeconds=%d, codeLength=%d, resendTime=%d, ttlNote=%s, otpJustSent=%s", user.getId(), expirySeconds, codeLength, resendTime, ttlNote, otpJustSent);

        if ("true".equals(otpJustSent)) {
            attributes.put("showOtpSentMessage", true);
            context.getAuthenticationSession().removeAuthNote(Constants.OTP_SENT_FLAG);
        }

        attributes.put(Constants.EXPIRY_SECONDS, expirySeconds);
        attributes.put(Constants.CODE_LENGTH, codeLength);
        attributes.put(Constants.RESEND_TIME, resendTime);
        attributes.put("expiryEpochMillis", ttlNote);
        // Initialize the form
        form = context.form().setExecution(context.getExecution().getId());
        // Add attributes (userEmail, mobileNumber, expirySeconds)
        if ((isAlwaysExecute || isEmailOtpEnabled) && !StringUtil.isNullOrEmpty(email)) {
            attributes.put("userEmail", email);
        }
        if (isSmsOtpEnabled && !StringUtil.isNullOrEmpty(mobileNumber)) {
            attributes.put("userMobileNumber", Utils.maskMobileNumber(mobileNumber));
        }

        if (error != null) {
            form.setAttribute(Constants.AUTH_ERROR_KEY, error);
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        form.setAttribute("attributes", attributes); // Pass all to Keycloakify
        return form.createForm("otp-form.ftl");
    }

    /**
     * Generates an OTP code and sends it to the user's email if not already sent.
     */
    private void generateAndSendEmailCode(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (session.getAuthNote(Constants.CODE) != null) { // skip sending email code
            log.infof("kcId=%s OTP code already generated and sent, skipping regeneration.", context.getUser().getId());
            return;
        }

        int ttl = Utils.getConfigInt(config, Constants.CODE_TTL, Constants.DEFAULT_TTL);
        int codeLength = Utils.getConfigInt(config, Constants.CODE_LENGTH, Constants.DEFAULT_LENGTH);

        String code = SecretGenerator.getInstance().randomString(codeLength, SecretGenerator.DIGITS);
        log.infof("kcId=%s code generated is : %s", context.getUser().getId(), code);
        session.setAuthNote(Constants.CODE, code);
        session.setAuthNote(Constants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
        session.setAuthNote(Constants.OTP_SENT_FLAG, "true");
        session.setAuthNote(Constants.CODE_ISSUED_AT, Utils.getCurrentTimestamp());
        // reset invalid attempts
        session.removeAuthNote(Constants.INVALID_ATTEMPTS);

        try {
            NotificationService.sendOtpAsync(context, code, ttl);
        } catch (Exception e) {
            log.errorf(e, "Failed to send OTP");
        }
    }

    /**
     * Handles user actions (form submission, resend, cancel).
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.OTP_SUBMITED, context.getFlowPath());

        UserModel userModel = context.getUser();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        if (!enabledUser(context, userModel)) {
            // error in context is set in enabledUser/isDisabledByBruteForce
            log.infof("kcId=%s User " + userModel.getUsername() + " is not enabled for OTP.", userModel.getId());
            return;
        }

        if (isDisabledByBruteForce(context, userModel)) {
            log.infof("kcId=%s User " + userModel.getUsername() + " is disabled by brute force protection.", userModel.getId());
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        log.infof("kcId=%s Form Data %s", userModel.getId(), formData);
        userModel.setSingleAttribute(UserModel.LOCALE, formData.getFirst(Constants.KC_LOCALE));

        if (formData.containsKey(Constants.RESEND)) {
            int maxResendAttempts = Utils.getConfigInt(config, Constants.MAX_RESEND_ATTEMPTS, Constants.DEFAULT_MAX_RESEND_ATTEMPTS);
            String resendCountNote = context.getAuthenticationSession().getAuthNote(Constants.RESEND_COUNT);
            int resendAttempts = (resendCountNote != null && !resendCountNote.isEmpty()) ? Utils.convertToInt(resendCountNote, Constants.DEFAULT_MAX_RESEND_ATTEMPTS) : 0;
            resendAttempts++;

            log.infof("kcId=%s User [%s] resendAttempts: %d", userModel.getId(), userModel.getUsername(), resendAttempts);

            if (resendAttempts > maxResendAttempts) {
                log.warnf("kcId=%s User [%s] has exceeded max OTP resend attempts (%d)", userModel.getId(), userModel.getUsername(), maxResendAttempts);

                publishFailedEvent(context, userModel, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_RESEND_MAX_REACHED);

                Response challengeResponse = renderOtpForm(context, Constants.OTP_RESEND_MAX_REACHED, Constants.RESEND_CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                return;
            }

            context.getAuthenticationSession().setAuthNote(Constants.RESEND_COUNT, String.valueOf(resendAttempts));
            resetEmailCode(context);
            generateAndSendEmailCode(context);
            log.infof("kcId=%s OTP code reset due to resend request.", userModel.getId());
            Response challengeResponse = renderOtpForm(context, null, null);
            context.challenge(challengeResponse);
            return;
        }

        if (formData.containsKey(Constants.CANCEL)) {
            log.infof("kcId=%s OTP process cancelled by user " + userModel.getUsername(), userModel.getId());
            Utils.clearAllOtpNotes(context);
            context.resetFlow();
            return;
        }

        AuthenticationSessionModel session = context.getAuthenticationSession();
        String code = session.getAuthNote(Constants.CODE);
        String ttl = session.getAuthNote(Constants.CODE_TTL);
        String enteredCode = formData.getFirst(Constants.OTP);
        // Basic presence checks
        if (StringUtil.isNullOrEmpty(code) || StringUtil.isNullOrEmpty(ttl) || StringUtil.isNullOrEmpty(enteredCode)) {
            log.infof("kcId=%s OTP code, TTL, or entered code is missing.", userModel.getId());

            publishFailedEvent(context, userModel, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_INVALID);

            Response challengeResponse = renderOtpForm(context, Constants.OTP_INVALID, Constants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return;
        }

        if (enteredCode.equals(code)) {
            if (Utils.convertToLong(ttl, 0) < System.currentTimeMillis()) {
                // expired
                log.infof("kcId=%s OTP code has expired.", userModel.getId());

                publishFailedEvent(context, userModel, Errors.EXPIRED_CODE, Constants.OTP_EXPIRED);

                Response challengeResponse = renderOtpForm(context, Constants.OTP_EXPIRED, Constants.CODE);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
            } else {
                // valid
                log.infof("kcId=%s OTP code is valid.", userModel.getId());
//                OtpUtils.clearBruteForceFailures(context, userModel);
                Utils.clearAllOtpNotes(context);
                context.success();
            }
        } else {
            // invalid
            log.infof("kcId=%s OTP code is invalid.", userModel.getId());
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                if (handleInvalidOtpAttempt(context, userModel)) {
                    log.infof("kcId=%s Max invalid OTP attempts reached, stopping flow(Too many attempts made. Request a new code).", userModel.getId());
                    return;
                }

                publishFailedEvent(context, userModel, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_INVALID);

                Response challengeResponse = renderOtpForm(context, Constants.OTP_INVALID, Constants.CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
    }

    @Override
    protected boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        String bruteForceError = AuthenticatorUtils.getDisabledByBruteForceEventError(context, user);
        if (bruteForceError != null) {
            log.infof("kcId=%s User " + user.getUsername() + " is disabled by brute force protection.", user.getId());

            publishFailedEvent(context, user, bruteForceError, Constants.OTP_BRUTE_FORCE_WAIT);

            RealmModel realm = context.getRealm();
            int resetSec = realm.getMaxDeltaTimeSeconds();
            int minutes = (resetSec + 59) / 60;

            String msg = String.format("Too many attempts. Try again in ~%d minute%s.", minutes, minutes == 1 ? "" : "s");

            LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
            form.setAttribute(Constants.WAIT_MINUTES, minutes);
            form.setAttribute(Constants.AUTH_ERROR_KEY, Constants.OTP_BRUTE_FORCE_WAIT);
            form.addError(new FormMessage(Constants.OTP_BRUTE_FORCE_WAIT));
            Response challengeResponse = form.createForm("otp-form.ftl");
            context.forceChallenge(challengeResponse);

            return true;
        }
        return false;
    }


    /**
     * Returns the error message shown when brute force protection disables login.
     */
//    @Override
//    protected String disabledByBruteForceError() {
//        return Messages.ACCOUNT_TEMPORARILY_DISABLED;
//    }

    /**
     * Removes the OTP code from the authentication session.
     */
    private void resetEmailCode(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        session.removeAuthNote(Constants.CODE);
        session.removeAuthNote(Constants.CODE_TTL);
        session.removeAuthNote(Constants.INVALID_ATTEMPTS);
    }

    /**
     * This authenticator requires a user to be identified.
     */
    @Override
    public boolean requiresUser() {
        return true;
    }

    /**
     * Returns true if user has an email address set.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user != null;
    }

    /**
     * No required actions are needed.
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    /**
     * Cleanup method (no operation).
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * Tracks invalid OTP attempts and enforces lockout if limit exceeded.
     *
     * @return true if max attempts reached (flow should stop), false otherwise
     */
    private boolean handleInvalidOtpAttempt(AuthenticationFlowContext context, UserModel userModel) {
        AuthenticationSessionModel session = context.getAuthenticationSession();

        String attemptsStr = session.getAuthNote(Constants.INVALID_ATTEMPTS);
        int attempts = attemptsStr == null ? 0 : Utils.convertToInt(attemptsStr, 0);
        attempts++;
        session.setAuthNote(Constants.INVALID_ATTEMPTS, String.valueOf(attempts));
        log.infof("kcId=%s Invalid OTP attempt %d/%d for user %s", userModel.getId(), attempts, Constants.MAX_INVALID_ATTEMPTS, userModel.getUsername());
        if (attempts > Constants.MAX_INVALID_ATTEMPTS) {
            log.infof("kcId=%s Max invalid OTP attempts reached for user " + userModel.getUsername() + ", resetting OTP code.", userModel.getId());
            resetEmailCode(context);

            publishFailedEvent(context, userModel, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_TOO_MANY_ATTEMPTS);

            Response challengeResponse = renderOtpForm(context, Constants.OTP_TOO_MANY_ATTEMPTS, Constants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return true;
        }
        return false;
    }

    /**
     * Sends the generated OTP code to the user's email using Keycloak's email provider.
     */
    private void sendEmailWithCode(KeycloakSession session, RealmModel realm, UserModel user, String code, int ttl) {
        if (user.getEmail() == null) {
            log.warnf("kcId=%s Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername(), user.getId());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);
        mailBodyAttributes.put("ttl", ttl);
        log.infof("kcId=%s going to send mailbody attributes: " + mailBodyAttributes, user.getId());

        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            log.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }

    /**
     * Builds a JSON error response with status code and error details.
     */
    private Response prepareErrorResponse(String errorCode, String errorDescription, int httpStatus) {
        Map<String, String> error = new HashMap<>();
        error.put("code", errorCode);
        error.put("message", errorDescription);

        return Response.status(httpStatus)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    public static void publishFailedEvent(AuthenticationFlowContext context,
                                          UserModel userModel,
                                          String error,
                                          String failureDetail) {
        var event = context.getEvent();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (userModel != null) {
            event.user(userModel);
        }

        event.detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(session));
        event.detail(Constants.AUTH_FLOW_ID, context.getExecution().getId());
        event.detail(Constants.FAILURE_DETAILS, failureDetail);
        event.detail(Constants.OTP_METHOD, Utils.resolveOtpMethod(userModel));
        event.detail(Constants.ATTEMPT_NUMBER, session.getAuthNote(Constants.INVALID_ATTEMPTS));
        event.detail(Constants.CODE_DELIVER_TO, userModel != null ? userModel.getEmail() : "null");
        event.detail(Constants.CODE_ISSUED_AT, session.getAuthNote(Constants.CODE_ISSUED_AT));
        event.detail(Constants.CODE_EXPIRY_SECONDS, Utils.getConfigString(context.getAuthenticatorConfig(), Constants.CODE_TTL, "300"));

        log.infof("kcId=%s Publishing OTP failure event: error=%s, failureDetail=%s, otpMethod=%s, attemptNumber=%s, codeIssuedAt=%s",
                userModel != null ? userModel.getId() : "null", error, failureDetail, Utils.resolveOtpMethod(userModel), session.getAuthNote(Constants.INVALID_ATTEMPTS), session.getAuthNote(Constants.CODE_ISSUED_AT));
        event.error(error);
    }
}
