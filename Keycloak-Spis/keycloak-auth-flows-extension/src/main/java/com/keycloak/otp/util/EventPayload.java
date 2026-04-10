package com.keycloak.otp.util;


import com.fasterxml.jackson.annotation.JsonProperty;

public class EventPayload {
    // metadata
    @JsonProperty("realmId")
    public String realmId;
    @JsonProperty("clientId")
    public String clientId;
    @JsonProperty("eventType")
    public String eventType;

    // user info
    @JsonProperty("userId")
    public String userId;
    @JsonProperty("UserName")
    public String userName;
    @JsonProperty("email")
    public String email;
    @JsonProperty("name")
    public String name;
    @JsonProperty("UserType")
    public String userType;
    @JsonProperty("PracticeId")
    public String practiceId;
    @JsonProperty("type")
    public String type;

    // session info
    @JsonProperty("id")
    public String id;
    @JsonProperty("sessionId")
    public String sessionId;
    @JsonProperty("status")
    public String status;
    @JsonProperty("sessionStart")
    public String sessionStart;
    @JsonProperty("lastActive")
    public String lastActive;
    @JsonProperty("timeZone")
    public String timeZone;

    // device info
    @JsonProperty("DeviceId")
    public String deviceId;
    @JsonProperty("DeviceOs")
    public String deviceOs;
    @JsonProperty("DeviceName")
    public String deviceName;
    @JsonProperty("DeviceType")
    public String deviceType;
    @JsonProperty("device")
    public String device;
    @JsonProperty("appType")
    public String appType;
    @JsonProperty("appVersion")
    public String appVersion;

    // network info
    @JsonProperty("clientIp")
    public String clientIp;
    @JsonProperty("serverIp")
    public String serverIp;
    @JsonProperty("serverPort")
    public String serverPort;
    @JsonProperty("userAgent")
    public String userAgent;

    // geo info
    @JsonProperty("GeoCity")
    public String geoCity;
    @JsonProperty("GeoCountry")
    public String geoCountry;

    @JsonProperty("errorCode")
    public String errorCode;
    @JsonProperty("failureReason")
    public String failureReason;

    public EventPayload() {
        // default ctor for Jackson
    }

    @Override
    public String toString() {
        return "EventPayload{" +
                "realmId='" + realmId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", userType='" + userType + '\'' +
                ", practiceId='" + practiceId + '\'' +
                ", type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", status='" + status + '\'' +
                ", sessionStart='" + sessionStart + '\'' +
                ", lastActive='" + lastActive + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceOs='" + deviceOs + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", device='" + device + '\'' +
                ", appType='" + appType + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", serverIp='" + serverIp + '\'' +
                ", serverPort='" + serverPort + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", geoCity='" + geoCity + '\'' +
                ", geoCountry='" + geoCountry + '\'' +
                '}';
    }
}