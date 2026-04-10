package com.keycloak.otp.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FailedEventLogRequest {

    @JsonProperty("authFlowId")
    public String authFlowId;

    @JsonProperty("attemptedEmail")
    public String attemptedEmail;

    @JsonProperty("attemptedPhone")
    public String attemptedPhone;

    @JsonProperty("resolvedUserId")
    public String resolvedUserId;

    @JsonProperty("resolvedUserName")
    public String resolvedUserName;

    @JsonProperty("resolvedUserEmail")
    public String resolvedUserEmail;

    @JsonProperty("resolvedUserType")
    public String resolvedUserType;

    @JsonProperty("attemptResult")
    public String attemptResult;

    @JsonProperty("failureDetail")
    public String failureDetail;

    @JsonProperty("systemErrorCode")
    public String systemErrorCode;

    @JsonProperty("twoFactor")
    public TwoFactorInfo twoFactor;

    @JsonProperty("sessionId")
    public String sessionId;

    @JsonProperty("clientId")
    public String clientId;

    @JsonProperty("clientIP")
    public String clientIP;

    @JsonProperty("clientPort")
    public String clientPort;

    @JsonProperty("userAgent")
    public String userAgent;

    @JsonProperty("appType")
    public String appType;

    @JsonProperty("appVersion")
    public String appVersion;

    @JsonProperty("deviceType")
    public String deviceType;

    @JsonProperty("deviceName")
    public String deviceName;

    @JsonProperty("deviceOS")
    public String deviceOS;

    public static class TwoFactorInfo {
        @JsonProperty("method")
        public String method;

        @JsonProperty("attemptNumber")
        public int attemptNumber;

        @JsonProperty("codeDeliveredTo")
        public String codeDeliveredTo;

        @JsonProperty("codeIssuedAt")
        public String codeIssuedAt;

        @JsonProperty("codeExpirySeconds")
        public int codeExpirySeconds;

        @Override
        public String toString() {
            return "TwoFactorInfo{" +
                    "method='" + method + '\'' +
                    ", attemptNumber=" + attemptNumber +
                    ", codeDeliveredTo='" + codeDeliveredTo + '\'' +
                    ", codeIssuedAt='" + codeIssuedAt + '\'' +
                    ", codeExpirySeconds=" + codeExpirySeconds +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "FailedEventLogRequest{" +
                "authFlowId='" + authFlowId + '\'' +
                ", attemptedEmail='" + attemptedEmail + '\'' +
                ", attemptedPhone='" + attemptedPhone + '\'' +
                ", resolvedUserId='" + resolvedUserId + '\'' +
                ", resolvedUserName='" + resolvedUserName + '\'' +
                ", resolvedUserEmail='" + resolvedUserEmail + '\'' +
                ", resolvedUserType='" + resolvedUserType + '\'' +
                ", attemptResult=" + attemptResult +
                ", failureDetail='" + failureDetail + '\'' +
                ", systemErrorCode='" + systemErrorCode + '\'' +
                ", twoFactor=" + twoFactor +
                ", sessionId='" + sessionId + '\'' +
                ", clientIP='" + clientIP + '\'' +
                ", clientPort='" + clientPort + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", appType='" + appType + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceOS='" + deviceOS + '\'' +
                '}';
    }
}
