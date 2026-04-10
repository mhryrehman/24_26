package com.keycloak.health;

public class HealthCheckResponse {
    private boolean passwordGrant;
    private boolean tokenExchange;
    private boolean rpt;

    public boolean isPasswordGrant() {
        return passwordGrant;
    }

    public void setPasswordGrant(boolean passwordGrant) {
        this.passwordGrant = passwordGrant;
    }

    public boolean isTokenExchange() {
        return tokenExchange;
    }

    public void setTokenExchange(boolean tokenExchange) {
        this.tokenExchange = tokenExchange;
    }

    public boolean isRpt() {
        return rpt;
    }

    public void setRpt(boolean rpt) {
        this.rpt = rpt;
    }

    public boolean isAllHealthy() {
        return passwordGrant && tokenExchange && rpt;
    }
}
