package com.keycloak.otp.auth.directgrant;

import com.keycloak.otp.service.NotificationService;
import com.keycloak.otp.util.Constants;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import com.keycloak.otp.util.Utils;


import java.util.HashMap;
import java.util.Map;

/**
 * Author: Yasir Rehman
 * <p>
 * This class provides a custom Authenticator implementation for Keycloak,
 * specifically designed for Direct Access Grant (Resource Owner Password Credentials Grant) flows.
 * It implements OTP (One-Time Password) verification via email.
 */
@JBossLog
public class OtpAuthenticatorDirectGrant implements Authenticator {

    private static final Logger log = Logger.getLogger(OtpAuthenticatorDirectGrant.class);

    /**
     * Main authentication logic for OTP verification during Direct Access Grant.
     * Handles generating, validating, and resending OTPs via email.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String username = user.getUsername();
        String mobileNumber = user.getFirstAttribute(Constants.MOBILE_NUMBER);

        log.infof("kcId=%s EmailAuthenticatorDirectGrant invoked for user: %s", user.getId(), username);

        if (!isUserEnabled(context)) {
            log.infof("kcId=%s User %s is temporarily disabled due to too many failed login attempts.",user.getId(), username);
            context.getEvent().user(context.getUser()).error(Errors.USER_TEMPORARILY_DISABLED);
            context.failureChallenge(AuthenticationFlowError.USER_TEMPORARILY_DISABLED, prepareResponse("user_temporarily_disabled", "Too many invalid attempts. Please wait and try again.", HttpStatus.SC_UNAUTHORIZED));
            return;
        }

        boolean isEmailOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_EMAIL_ENABLED));
        boolean isSmsOtpEnabled = Boolean.parseBoolean(user.getFirstAttribute(Constants.OTP_SMS_ENABLED));

        if (!isEmailOtpEnabled && !isSmsOtpEnabled) {
            log.infof("kcId=%s Skipping OTP for user %s (OTP not enabled via email or SMS)",user.getId(), username);
            context.success();
            return;
        }

        // Parse form parameters
        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
        String enteredOtp = formParams.getFirst(Constants.OTP);
        boolean isResendRequested = "true".equalsIgnoreCase(formParams.getFirst(Constants.RESEND_OTP));

        if (isResendRequested) {
            log.infof("kcId=%s Resend OTP requested by user: %s", user.getId(), username);

            int maxResendAllowed = getMaxResendAttempts(context);
            int resendCount = getResendCount(user);

            if (resendCount >= maxResendAllowed) {
                log.warnf("kcId=%s User %s has exceeded the maximum OTP resend attempts (%d)", user.getId(), username, maxResendAllowed);
                context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        prepareResponse("otp_resend_limit",
                                "You have exceeded the maximum number of allowed OTP resends. Please try again later.",
                                HttpStatus.SC_TOO_MANY_REQUESTS));
                return;
            }

            generateAndSendEmailCode(context);
            incrementResendCount(user, resendCount);

            context.challenge(prepareResponse("otp_resent", "A new OTP has been sent to your email.", HttpStatus.SC_OK));
            return;
        }

        // Get stored OTP and expiry
        String storedOtp = user.getFirstAttribute(Constants.CODE);
        String expiryStr = user.getFirstAttribute(Constants.CODE_TTL);

        // If no OTP entered or generated yet, trigger it
        if (StringUtil.isNullOrEmpty(storedOtp) || StringUtil.isNullOrEmpty(enteredOtp)) {
            log.infof("kcId=%s OTP missing or not entered for user: %s, generating...", user.getId(), username);
            generateAndSendEmailCode(context);
            if(isEmailOtpEnabled && isSmsOtpEnabled) {
                log.infof("kcId=%s Both Email and SMS OTP enabled for user: %s", user.getId(), username);
                context.challenge(prepareResponse("otp_required", "Enter the code sent to your cell ending in " + Utils.maskMobileNumber(mobileNumber) + " and email " + Utils.maskEmail(username) + ".", HttpStatus.SC_OK));
            }
            else if(isEmailOtpEnabled) {
                log.infof("kcId=%s Email OTP enabled for user: %s", user.getId(), username);
                context.challenge(prepareResponse("otp_required", "Enter the code sent to your email " + Utils.maskEmail(username) + ".", HttpStatus.SC_OK));
            }
            else {
                log.infof("kcId=%s SMS OTP enabled for user: %s", user.getId(), username);
                context.challenge(prepareResponse("otp_required", "Enter the code sent to your cell ending in " + Utils.maskMobileNumber(mobileNumber) + ".", HttpStatus.SC_OK));
            }
            return;
        }

        // Validate OTP
        if (!enteredOtp.equals(storedOtp)) {
            log.warnf("kcId=%s Invalid OTP entered by user: %s", user.getId(), username);
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    prepareResponse("invalid_otp", "The OTP you provided is incorrect. Please try again.",
                            HttpStatus.SC_UNAUTHORIZED));
            return;
        }

        // Check expiry
        try {
            long expiry = Long.parseLong(expiryStr);
            if (System.currentTimeMillis() > expiry) {
                log.warnf("kcId=%s Expired OTP for user: %s", user.getId(), username);
                resetOtpAttributes(user);
                context.getEvent().user(user).error(Errors.EXPIRED_CODE);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        prepareResponse("expired_otp", "Your OTP has expired.",
                                HttpStatus.SC_UNAUTHORIZED));
                return;
            }
        } catch (NumberFormatException e) {
            log.errorf("kcId=%s Invalid expiry timestamp for OTP of user: %s", user.getId(), username);
            resetOtpAttributes(user);
            context.failureChallenge(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR,
                    prepareResponse("otp_error", "Something went wrong with OTP validation. Please try again.",
                            HttpStatus.SC_UNAUTHORIZED));
            return;
        }

        log.infof("kcId=%s OTP validated successfully for user: %s", user.getId(), username);
        resetOtpAttributes(user);
        resetResendCount(user);
        context.success();
    }

    /**
     * This method is not used in Direct Access Grant flows.
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used in Direct Access Grant flows
    }

    /**
     * Indicates this authenticator requires a user to be already identified.
     */
    @Override
    public boolean requiresUser() {
        return true;
    }

    /**
     * Checks if the user is configured to receive OTP via email.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getEmail() != null;
    }

    /**
     * No required actions are set during this authentication step.
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    /**
     * Performs any necessary cleanup (not used).
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * Generates an OTP code, stores it in the user attributes, and sends it to the user's email.
     */
    private void generateAndSendEmailCode(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        int length = Constants.DEFAULT_LENGTH;
        int ttl = Constants.DEFAULT_TTL;

        if (config != null) {
            length = Integer.parseInt(config.getConfig().getOrDefault(Constants.CODE_LENGTH, String.valueOf(length)));
            ttl = Integer.parseInt(config.getConfig().getOrDefault(Constants.CODE_TTL, String.valueOf(ttl)));
        }

        String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
        long expiryTimeMillis = System.currentTimeMillis() + (ttl * 1000L);

        log.infof("kcId=%s Generated OTP for user %s: %s (expires in %d seconds)", user.getId(), user.getUsername(), code, ttl);

        //this method will send email using Keycloak's SMTP server.
//        sendEmailWithCode(context.getSession(), context.getRealm(), user, code, ttl);

        session.setAuthNote(Constants.CODE, code);
        session.setAuthNote(Constants.CODE_TTL, String.valueOf(expiryTimeMillis));
        user.setSingleAttribute(Constants.CODE, code);
        user.setSingleAttribute(Constants.CODE_TTL, String.valueOf(expiryTimeMillis));

        try {
            NotificationService.sendOtpAsync(context, code, ttl);
        } catch (Exception e) {
            log.errorf(e, "Failed to send OTP to user [%s]", user.getUsername());
        }
    }

    /**
     * Retrieves the maximum allowed resend attempts from configuration.
     */
    private int getMaxResendAttempts(AuthenticationFlowContext context) {
        String value = context.getAuthenticatorConfig() != null
                ? context.getAuthenticatorConfig().getConfig().getOrDefault(Constants.MAX_RESEND_ATTEMPTS, "3")
                : "3";
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid maxResendAttempts config, using default value 3");
            return 3;
        }
    }

    /**
     * Retrieves the current number of OTP resends for the user.
     */
    private int getResendCount(UserModel user) {
        String count = user.getFirstAttribute(Constants.RESEND_COUNT);
        try {
            return count != null ? Integer.parseInt(count) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Increments the resend counter stored in user attributes.
     */
    private void incrementResendCount(UserModel user, int currentCount) {
        user.setSingleAttribute(Constants.RESEND_COUNT, String.valueOf(currentCount + 1));
    }

    /**
     * Resets the resend counter by removing the attribute.
     */
    private void resetResendCount(UserModel user) {
        user.removeAttribute(Constants.RESEND_COUNT);
    }

    /**
     * Removes OTP-related attributes from the user.
     */
    private void resetOtpAttributes(UserModel user) {
        user.removeAttribute(Constants.CODE);
        user.removeAttribute(Constants.CODE_TTL);
    }
    /**
     * Builds a JSON error response with status code and error details.
     */
    private Response prepareResponse(String errorCode, String errorDescription, int httpStatus) {
        Map<String, String> response = new HashMap<>();
        response.put("code", errorCode);
        response.put("message", errorDescription);

        return Response.status(httpStatus)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private boolean isUserEnabled(AuthenticationFlowContext context) {
        BruteForceProtector protector = context.getSession().getProvider(BruteForceProtector.class);
        return !protector.isTemporarilyDisabled(context.getSession(), context.getRealm(), context.getUser())
                && !protector.isPermanentlyLockedOut(context.getSession(), context.getRealm(), context.getUser());
    }
}
