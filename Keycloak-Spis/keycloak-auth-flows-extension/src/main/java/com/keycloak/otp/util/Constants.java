package com.keycloak.otp.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: Yasir Rehman
 * <p>
 * Utility class containing constants used throughout the email OTP authentication logic.
 * This includes configuration keys, default values, and parameter names used for storing
 * and processing OTPs in Keycloak authentication flows.
 */
@UtilityClass
public class Constants {
    public static String CODE = "emailCode";

    public static String RESEND_CODE = "resendCode";

    public static String OTP = "otp";

    public static String CANCEL = "cancel";

    public static String RESEND = "resend";

    public static String RESEND_OTP = "resend_otp";

    public static String OTP_EMAIL_ENABLED = "otp_email_enabled";

    public static String OTP_SMS_ENABLED = "otp_sms_enabled";

    public static final String RESEND_COUNT = "otp_resend_count";

    public static final String ALWAYS_EXECUTE = "always_execute";

    public static String CODE_LENGTH = "length";

    public static String CODE_TTL = "ttl";
    public static String INVALID_ATTEMPTS = "invalidAttempts";
    public static String OTP_SENT_FLAG = "otpSentFlag";

    public static final String MAX_RESEND_ATTEMPTS = "maxResendAttempts";
    public static final String RESEND_TIME = "resendTime";
    public static final int MAX_INVALID_ATTEMPTS = 3;

    public static int DEFAULT_LENGTH = 4;

    public static int DEFAULT_TTL = 300;

    public static int DEFAULT_MAX_RESEND_ATTEMPTS = 3;
    public static int DEFAULT_RESEND_TIME= 60;

    public static final String NOVU_URI = "novuUri";

    public static final String NOVU_API_KEY = "novuApiKey";

    public static final String WORK_FLOW_NAME = "workflowName";

    public static final String TOKEN_REQUEST_BODY = "tokenRequestBody";

    public static final String TOKEN_URL = "tokenUrl";

    public static final String SMS_URL = "SMSUrl";

    public static final String MESSAGE_TEXT = "messageText";

    public static final String MESSAGE_TYPE = "messageType";

    public static final String MOBILE_NUMBER = "mobile_number";

    public static final String EMAIL_SUBJECT_TEMPLATE = "emailSubjectTemplate";
    public static final String EMAIL_TEXT_BODY_TEMPLATE = "emailTextBodyTemplate";
    public static final String EMAIL_HTML_BODY_TEMPLATE = "emailHtmlBodyTemplate";

    public static final String EXPIRY_SECONDS = "expirySeconds";
    public static final String CFG_PARAM_NAME = "paramName";
    public static final String CFG_PARAM_VALUE = "paramValue";
    public static final String QP_PREPEND = "reg.qp.";
    public static final String NOTE_ATTRS_JSON = "reg.attrs.json";

    public static final String THREAD_POOL_CORE_SIZE = "thread.pool.core.size";
    public static final String THREAD_POOL_MAX_SIZE = "thread.pool.max.size";
    public static final String THREAD_POOL_QUEUE_SIZE = "thread.pool.queue.size";
    public static final String DEFAULT_THREAD_POOL_CONFIG_PATH = "/opt/bitnami/keycloak/providers/email-thread-config.properties";
    public static final String THREAD_POOL_CONFIG_PATH = "THREAD_POOL_CONFIG_PATH";

    public static final int DEFAULT_CORE_POOL_SIZE = 10;
    public static final int DEFAULT_MAX_POOL_SIZE = 30;
    public static final int DEFAULT_QUEUE_SIZE = 50;

    public static final String EULA_ACCEPTED = "eula_accepted";
    public static final String FORM_PARAM_EULA_ACCEPTED = "user.attributes.eula_accepted";

    public static final String OHA_ACCEPTED = "oha_accepted";
    public static final String FORM_PARAM_OHA_ACCEPTED = "user.attributes.oha_accepted";

    public static final String LOG_TOKEN_URL = "log_token_url";
    public static final String LOG_CLIENT_ID = "log_client_id";
    public static final String LOG_CLIENT_SECRET = "log_client_secret";
    public static final String LOG_API_URL = "log_api_url";
    public static final String LOG_SESSION_ID = "log_session_id";
    public static final String LEAP_REG_API_URL = "reg_api_url";
    public static final String LOG_SESSION_URL = "log_session_url";
    public static final String LEAP_RP_REG_API_URL = "rp_reg_api_url";
    public static final String LEAP_LOG_FAILED_SESSION_URL = "log_failed_session_url";

    public static final String LOGIN_FORM_SUBMITED = "LOGIN_FORM_SUBMITED";
    public static final String OTP_SUBMITED = "OTP_SUBMITED";
    public static final String SIGNUP_OTP_SUBMITED = "SIGNUP_OTP_SUBMITED";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_SELECTED = "USER_SELECTED";
    public static final String ACTION_EULA_ACCEPTED = "EULA_ACCEPTED";
    public static final String OHA_CONSENT = "OHA_CONSENT";

    public static final Set<String> PRACTICE_ADMINS = new HashSet<>(Arrays.asList("Practitioner", "SuperUser"));
    public static final String USER_TYPE = "user_type";

    public static final String INVITATION_ID = "invitationId";
    public static final String ACTIVATION_CODE_PROFILE = "activationCodeProfile";
    public static final String MALE = "Male";
    public static final String FEMALE = "Female";
    public static final String Mr = "Mr";
    public static final String Mrs = "Mrs";
    public static final String Mx = "Mx";
    public static final int CONNECT_TIMEOUT_MILLIS = 10000;
    public static final int SOCKET_TIMEOUT_MILLIS = 15000;
    public static final int RESP_BODY_SIZE_MAX = 512;

    public static final String OTP_INVALID = "otpInvalid";
    public static final String OTP_EXPIRED = "otpExpired";
    public static final String OTP_TOO_MANY_ATTEMPTS = "otpTooManyAttempts";
    public static final String OTP_RESEND_MAX_REACHED = "otpResendMaxReached";
    public static final String OTP_BRUTE_FORCE_WAIT = "otpBruteForceWait";
    public static final String WAIT_MINUTES = "waitMinutes";
    public static final String REGISTRATION_EXTERNAL_ERROR = "registrationExternalError";
    public static final String USERNAME_OR_EMAIL_INVALID      = "usernameOrEmailInvalid";
    public static final String EMAIL_INVALID      = "invalidEmail";
    public static final String ACCOUNT_DISABLED_CONTACT_ADMIN = "accountDisabledContactAdmin";
    public static final String AUTH_ERROR_KEY = "authErrorKey";
    public static final String KC_LOCALE = "kc_locale";

    public static final String FIELD_EMAIL_OR_PHONE_NUMBER = "emailOrMobile";
    public static final String FIELD_USER_NAME = "username";
    public static final String PHONE_NUMBER_ALREADY_EXISTS = "phoneNumberAlreadyExists";
    public static final String REQUIRED_EMAIL_OR_PHONE_NUMBER = "requiredEmailOrPhoneNumber";
    public static final String REQUIRED_PHONE_NUMBER = "requiredPhoneNumber";
    public static final String INVALID_PHONE_NUMBER = "invalidPhoneNumber";
    public static final String PHONE_NUMBER_REGEX = "^\\+?[1-9]\\d{9,14}$";

    public static final String FAILURE_DETAILS = "failure_details";
    public static final String AUTH_FLOW_ID = "auth_flow_id";
    public static final String AUTH_STEP = "AUTH_STEP";
    public static final String EMAIL = "email";
    public static final String BOTH = "both";
    public static final String OTP_METHOD = "otp_method";
    public static final String ATTEMPT_NUMBER = "attempt_number";
    public static final String CODE_DELIVER_TO = "code_deliver_to";
    public static final String CODE_ISSUED_AT = "code_issued_at";
    public static final String CODE_EXPIRY_SECONDS = "code_expiry_seconds";

    public static final String NOTE_CLIENT_IP = "note_client_ip";
    public static final String CLIENT_IP = "clientIp";
    public static final String NOTE_PASSWORD = "reg.password";
    public static final String NOTE_EMAIL = "reg.email";
    public static final String NOTE_USERNAME = "reg.username";
    public static final String NOTE_FIRST = "reg.firstName";
    public static final String NOTE_LAST = "reg.lastName";
    public static final String NOTE_TAC = "reg.tacAccepted";
    public static final String NOTE_EMAIL_OR_MOBILE = "reg.emailOrMobile";
    public static final String NOTE_PROFILE = "reg.profile";
    public static final String CAPTURE_QUERY_PARAMS = "captureQueryParams";
    public static final String CLIENT_REQUEST_PARAM_ = "client_request_param_";
    public static final String PAR_REF = "par_ref";

    public static final String TITLE = "title";
    public static final String ADDRESS_1 = "address_1";
    public static final String ADDRESS_2 = "address_2";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String COUNTRY = "country";
    public static final String ZIPCODE = "zipcode";
    public static final String IDENTIFIER_SYSTEM = "identifier_system";
    public static final String IDENTIFIER_USE = "identifier_use";
    public static final String IDENTIFIER_VALUE = "identifier_value";

    public static final String RP_FIRST_NAME = "rp_first_name";
    public static final String RP_LAST_NAME = "rp_last_name";
    public static final String RP_TITLE = "rp_title";
    public static final String RP_DOB = "rp_dob";
    public static final String RP_GENDER = "rp_gender";
    public static final String RP_EMAIL = "rp_email";
    public static final String RP_IDENTIFIER_SYSTEM = "rp_identifier_system";
    public static final String RP_IDENTIFIER_USE = "rp_identifier_use";
    public static final String RP_IDENTIFIER_VALUE = "rp_identifier_value";
    public static final String RP_ADDRESS_1 = "rp_address_1";
    public static final String RP_ADDRESS_2 = "rp_address_2";
    public static final String RP_CITY = "rp_city";
    public static final String RP_STATE = "rp_state";
    public static final String RP_ZIPCODE = "rp_zipcode";
    public static final String RP_COUNTRY = "rp_country";

    public static final String INVALID_CREDENTIALS = "InvalidCredentials";
    public static final String ACCOUNT_NOT_FOUND = "AccountNotFound";
    public static final String ACCOUNT_LOCKED = "AccountLocked";
    public static final String ACCOUNT_INACTIVE = "AccountInactive";
    public static final String ACCOUNT_SUSPENDED = "AccountSuspended";
    public static final String TWO_FACTOR_FAILED = "TwoFactorFailed";
    public static final String TWO_FACTOR_EXPIRED = "TwoFactorExpired";
    public static final String TWO_FACTOR_MAX_ATTEMPTS = "TwoFactorMaxAttempts";
    public static final String RATE_LIMITED = "RateLimited";
    public static final String SYSTEM_ERROR = "SystemError";

    public static final String INVALID_REGISTRATION_DATA = "invalidRegistrationData";
    public static final String USERNAME_ALREADY_EXISTS = "userNameAlreadyExists";
    public static final String EMAIL_ALREADY_EXISTS = "emailAlreadyExists";
    public static final String FAILED_TO_SET_PASSWORD = "failedToSetPassword";
    public static final String LEAP_REGISTRATION_FAILED = "leapRegistrationFailed";
    public static final String INTERNAL_ERROR = "internalError";
    public static final String MISSING_USERNAME = "missingUsername";
    public static final String USER_NOT_FOUND = "userNotFound";
    public static final String INVALID_PASSWORD_EXISTING = "invalidPasswordExisting";
    public static final String PASSWORD_POLICY_VALIDATION_FAILED = "passwordPolicyValidationFailed";
    public static final String INVALID_USERNAME = "invalidUsername";
    public static final String INVALID_PASSWORD = "invalidPassword";
    public static final String USER_DISABLED = "invalidUser";
    public static final String USERNAME_OR_EMAIL = "usernameOrEmail";


}
