package com.keycloak.otp.auth.register;

import com.keycloak.otp.model.UserDto;
import com.keycloak.otp.service.LeapIntegrationService;
import com.keycloak.otp.service.NotificationService;
import com.keycloak.otp.util.Constants;
import com.keycloak.otp.util.Utils;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Yasir Rehman
 * <p>
 * This class is a form-based authenticator used in browser-based signup flows.
 * It sends an OTP code to the user's email and validates it against the form input.
 */
@JBossLog
public class SignupOtpAuthenticatorForm extends AbstractUsernameFormAuthenticator {

    private static final Logger log = Logger.getLogger(SignupOtpAuthenticatorForm.class);

    /**
     * Starts the authentication process. If OTP is enabled, triggers challenge.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String email = resolveEmail(context);
        log.infof("Signup OTP Authenticator invoked for email: %s", email);

        if (matchQueryParam(context)) {
            log.infof("Skipping OTP as query param condition matched for email: %s", email);
            context.success();
            return;
        }

        generateAndSendEmailCode(context, email);
        Response challengeResponse = renderOtpForm(context, null, null);
        context.challenge(challengeResponse);
    }

    private Response renderOtpForm(AuthenticationFlowContext context, String error, String field) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());

        String email = resolveEmail(context);
        log.infof("Rendering OTP form for email: %s", email);
        if (StringUtil.isNullOrEmpty(email)) {
            log.warn("OTP form missing email in auth notes");
            return context.form()
                    .setError(Messages.INVALID_REQUEST)
                    .createErrorPage(Response.Status.BAD_REQUEST);
        }

        int expirySeconds = Utils.getConfigInt(config, Constants.CODE_TTL, Constants.DEFAULT_TTL);
        int codeLength = Utils.getConfigInt(config, Constants.CODE_LENGTH, Constants.DEFAULT_LENGTH);
        int resendTime = Utils.getConfigInt(config, Constants.RESEND_TIME, Constants.DEFAULT_RESEND_TIME);
        log.info("Expiry seconds: " + expirySeconds + ", Code length: " + codeLength + ", Resend time: " + resendTime);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Constants.EXPIRY_SECONDS, expirySeconds);
        attributes.put(Constants.CODE_LENGTH, codeLength);
        attributes.put(Constants.RESEND_TIME, resendTime);
        attributes.put("userEmail", email);
        form.setAttribute("attributes", attributes);

        if (error != null) {
            form.setAttribute(Constants.AUTH_ERROR_KEY, error);
            if (field != null) form.addError(new FormMessage(field, error));
            else form.setError(error);
        }
        return form.createForm("otp-form.ftl");
    }

    public boolean matchQueryParam(AuthenticationFlowContext context) {
        AuthenticatorConfigModel cfg = context.getAuthenticatorConfig();
        String name = Utils.getConfigString(cfg, Constants.CFG_PARAM_NAME, null);
        if (name == null || name.isBlank()) {
            log.infof("no param name configured, will run OTP");
            return true; // no config => run OTP
        }
        log.infof("checking for query param [%s] in default note.", context.getAuthenticationSession().getClientNote(Constants.CLIENT_REQUEST_PARAM_ + name));

        String actual = context.getAuthenticationSession().getAuthNote(Constants.QP_PREPEND + name);
        log.info("checking param [" + name + "] actual value: " + actual);
        String expected = Utils.getConfigString(cfg, Constants.CFG_PARAM_VALUE, null);

        boolean matches;
        if (StringUtil.isNullOrEmpty(actual)) {
            matches = false;
        } else if (expected == null || expected.isBlank()) {
            matches = true;
        } else {
            matches = expected.equalsIgnoreCase(actual);
        }

        return matches;
    }


    /**
     * Generates an OTP code and sends it to the user's email if not already sent.
     */
    private void generateAndSendEmailCode(AuthenticationFlowContext context, String email) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (session.getAuthNote(Constants.CODE) != null) {
            // skip sending email code
            log.infof("OTP code already generated for email: %s, skipping generation", email);
            return;
        }

        int ttl = Utils.getConfigInt(config, Constants.CODE_TTL, Constants.DEFAULT_TTL);
        int codeLength = Utils.getConfigInt(config, Constants.CODE_LENGTH, Constants.DEFAULT_LENGTH);
        String code = SecretGenerator.getInstance().randomString(codeLength, SecretGenerator.DIGITS);

        log.infof("Generated OTP code for email: %s, code: %s, ttl: %d seconds", email, code, ttl);
        session.setAuthNote(Constants.CODE, code);
        session.setAuthNote(Constants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
        session.setAuthNote(Constants.CODE_ISSUED_AT, Utils.getCurrentTimestamp());

        try {
            String username = session.getAuthNote(Constants.NOTE_USERNAME);
            String first = session.getAuthNote(Constants.NOTE_FIRST);
            String last = session.getAuthNote(Constants.NOTE_LAST);
            UserDto dto = new UserDto(username, email, first, last, null);
            if (Utils.isEmail(email)) {
                log.infof("Sending OTP code to email: %s", email);
                NotificationService.sendOtpAsyncEmail(context, dto, code, ttl);
            } else {
                log.infof("Sending OTP code to mobile: %s", email);
                dto.setPhoneNumber(email);
                NotificationService.sendOtpAsyncSms(context, dto, code, ttl);
            }
        } catch (Exception e) {
            log.errorf(e, "Failed to send OTP");
        }
    }

    /**
     * Handles user actions (form submission, resend, cancel).
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        LeapIntegrationService uals = new LeapIntegrationService(context.getSession());
        uals.logActivity(context.getAuthenticationSession(), Constants.SIGNUP_OTP_SUBMITED, context.getFlowPath());


        String email = resolveEmail(context);

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        log.info("Form Data" + formData);

        if (formData.containsKey(Constants.RESEND)) {
            int maxResendAttempts = Utils.getConfigInt(config, Constants.MAX_RESEND_ATTEMPTS, Constants.DEFAULT_MAX_RESEND_ATTEMPTS);
            String resendCountNote = context.getAuthenticationSession().getAuthNote(Constants.RESEND_COUNT);
            int resendAttempts = (resendCountNote != null && !resendCountNote.isEmpty()) ? Utils.convertToInt(resendCountNote, Constants.DEFAULT_MAX_RESEND_ATTEMPTS) : 0;
            resendAttempts++;

            log.infof("User [%s] resendAttempts: %d", email, resendAttempts);

            if (resendAttempts > maxResendAttempts) {
                log.warnf("User [%s] has exceeded max OTP resend attempts (%d)", email, maxResendAttempts);
                publishFailedEvent(context, email, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_RESEND_MAX_REACHED);
                Response challengeResponse = renderOtpForm(context, Constants.OTP_RESEND_MAX_REACHED, Constants.RESEND_CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                return;
            }

            context.getAuthenticationSession().setAuthNote(Constants.RESEND_COUNT, String.valueOf(resendAttempts));
            resetEmailCode(context);
            generateAndSendEmailCode(context, email);
            log.infof("User [%s] OTP resent successfully", email);
            Response challengeResponse = renderOtpForm(context, null, null);
            context.challenge(challengeResponse);
            return;
        }

        if (formData.containsKey(Constants.CANCEL)) {
            log.info("User cancelled OTP process.");
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
            log.info("OTP code, TTL, or entered code is missing.");
            publishFailedEvent(context, email, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_INVALID);

            Response challengeResponse = renderOtpForm(context, Constants.OTP_INVALID, Constants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return;
        }

        if (enteredCode.equals(code)) {
            if (Utils.convertToLong(ttl, 0) < System.currentTimeMillis()) {
                // expired
                log.infof("OTP code is expired for email: %s", email);
                publishFailedEvent(context, email, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_EXPIRED);

                Response challengeResponse = renderOtpForm(context, Constants.OTP_EXPIRED, Constants.CODE);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
            } else {
                // valid
                log.infof("OTP code is valid for email: %s", email);
                Utils.clearAllOtpNotes(context);
                context.success();
            }
        } else {
            // invalid
            log.infof("OTP code is invalid for email: %s", email);
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                if (handleInvalidOtpAttempt(context, email)) {
                    log.infof("Max invalid OTP attempts reached, stopping flow(Too many attempts made. Request a new code) for email: %s", email);
                    return;
                }
                publishFailedEvent(context, email, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_INVALID);

                Response challengeResponse = renderOtpForm(context, Constants.OTP_INVALID, Constants.CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
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
        return false;
    }

    /**
     * Returns true if user has an email address set.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user == null || (user.getEmail() != null && !user.getEmail().isBlank());
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
    private boolean handleInvalidOtpAttempt(AuthenticationFlowContext context, String email) {
        AuthenticationSessionModel session = context.getAuthenticationSession();

        String attemptsStr = session.getAuthNote(Constants.INVALID_ATTEMPTS);
        int attempts = attemptsStr == null ? 0 : Utils.convertToInt(attemptsStr, 0);
        attempts++;
        session.setAuthNote(Constants.INVALID_ATTEMPTS, String.valueOf(attempts));
        log.infof("Invalid OTP attempt %d/%d for user %s", attempts, Constants.MAX_INVALID_ATTEMPTS, email);
        if (attempts > Constants.MAX_INVALID_ATTEMPTS) {
            log.info("Max invalid OTP attempts reached for user " + email + ", resetting OTP code.");
            publishFailedEvent(context, email, Errors.INVALID_USER_CREDENTIALS, Constants.OTP_TOO_MANY_ATTEMPTS);

            resetEmailCode(context);
            Response challengeResponse = renderOtpForm(context, Constants.OTP_TOO_MANY_ATTEMPTS, Constants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return true;
        }
        return false;
    }

    private String resolveEmail(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        String email = session != null ? session.getAuthNote(Constants.NOTE_EMAIL) : null;
        if (StringUtil.isNullOrEmpty(email)) {
            email = session != null ? session.getAuthNote(Constants.NOTE_EMAIL_OR_MOBILE) : null;
        }
        return email;
    }


    public static void publishFailedEvent(AuthenticationFlowContext context,
                                          String userNameOrEmail,
                                          String error,
                                          String failureDetail) {
        var event = context.getEvent();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        event.detail(Constants.CLIENT_IP, Utils.getClientIpFromAuthSession(session));
        event.detail(Constants.USERNAME_OR_EMAIL, userNameOrEmail);
        event.detail(Constants.AUTH_FLOW_ID, context.getExecution().getId());
        event.detail(Constants.FAILURE_DETAILS, failureDetail);
        event.detail(Constants.OTP_METHOD, Constants.EMAIL);
        event.detail(Constants.ATTEMPT_NUMBER, session.getAuthNote(Constants.INVALID_ATTEMPTS));
        event.detail(Constants.CODE_DELIVER_TO, userNameOrEmail);
        event.detail(Constants.CODE_ISSUED_AT, session.getAuthNote(Constants.CODE_ISSUED_AT));
        event.detail(Constants.CODE_EXPIRY_SECONDS, Utils.getConfigString(context.getAuthenticatorConfig(), Constants.CODE_TTL, "300"));

        log.infof("Publishing failed OTP event for user [%s], error: %s, failureDetail: %s, attemptNumber: %s", userNameOrEmail, error, failureDetail, session.getAuthNote(Constants.INVALID_ATTEMPTS));
        event.error(error);
    }
}
