package com.keycloak.security.extensions.common;

public class Constant {
        public static final String PROVIDER_ID = "profile-cookie-authenticator";
        public static final String DISPLAY_TYPE = "Profile Cookie Authenticator";
        public static final String HELP_TEXT = "Stores user's name and email in a client-accessible cookie.";

        public static final String PROP_COOKIE_NAME = "cookieName";
        public static final String PROP_COOKIE_MAX_AGE = "cookieMaxAgeSeconds";
        public static final String PROP_COOKIE_PATH = "cookiePath";
        public static final String PROP_HTTP_ONLY = "cookieHttpOnly";
        public static final String PROP_COOKIE_COMMENT = "cookieComment";

        public static final String DEFAULT_COOKIE_NAME = "user_profiles";
        public static final String DEFAULT_COOKIE_PATH = "/";
        public static final String DEFAULT_COOKIE_COMMENT = "User profile list";
        public static final String DEFAULT_COOKIE_MAX_AGE = "2592000"; // 30 days
        public static final String DEFAULT_HTTP_ONLY = "false";

        public static final String FORM_TEMPLATE = "auto-submit.ftl";

        public static final String GRANT_TYPE = "grant_type";
        public static final String PASSWORD = "password";
        public static final String USER_ATTRIBUTE_NAME = "userAttributeName";
        public static final String DEVICE_QUERY_PARAM_NAME = "deviceQueryParamName";
        public static final String DEVICE_ID = "device_id";
        public static final String DEVICE_ADDRESSES = "device_addresses";
        public static final String NOVU_URI = "novuUri";
        public static final String NOVU_API_KEY = "novuApiKey";
        public static final String WORK_FLOW_NAME = "workflowName";

}
