package com.keycloak.security.extensions.common;

public class DeviceInfo {
    private String deviceType;           // desktop | mobile | tablet | other
    private String os;                   // Windows | macOS | Android | iOS | Other
    private String osMajor;              // e.g., 10, 11, 17, 14
    private String browserFamily;        // Edge | Chrome | Firefox | Safari | Other
    private String browserVersionMajor;  // e.g., 139
    private String arch;                 // x64 | arm64 | (empty)
    private String uaRaw;                // optional, for audit
    private String ipAddress;

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getOsMajor() { return osMajor; }
    public void setOsMajor(String osMajor) { this.osMajor = osMajor; }
    public String getBrowserFamily() { return browserFamily; }
    public void setBrowserFamily(String browserFamily) { this.browserFamily = browserFamily; }
    public String getBrowserVersionMajor() { return browserVersionMajor; }
    public void setBrowserVersionMajor(String browserVersionMajor) { this.browserVersionMajor = browserVersionMajor; }
    public String getArch() { return arch; }
    public void setArch(String arch) { this.arch = arch; }
    public String getUaRaw() { return uaRaw; }
    public void setUaRaw(String uaRaw) { this.uaRaw = uaRaw; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String fingerprint() {
        return String.join("|",
                safe(deviceType),
                safe(os),
                safe(osMajor),
                safe(browserFamily),
                safe(browserVersionMajor),
                safe(arch));
    }
    private String safe(String v) { return v == null ? "" : v; }

}
