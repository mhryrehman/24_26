package com.keycloak.otp.logut;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;

public class ForceLogoutResourceProvider implements RealmResourceProvider {
    private static final Logger LOG = Logger.getLogger(ForceLogoutResourceProvider.class);
    private final KeycloakSession session;

    public ForceLogoutResourceProvider(KeycloakSession session) { this.session = session; }
    @Override public Object getResource() { return this; }
    @Override public void close() {}

    /** Helper: add a Set-Cookie that deletes a cookie on a specific path. */
    private static void addExpireCookie(Response.ResponseBuilder rb, String name, String path, boolean secure) {
        rb.cookie(new NewCookie(
                name,            // name
                "",              // value
                path,            // path must match original
                null,            // domain (host-only)
                null,            // comment
                0,               // Max-Age=0 => delete
                true,            // HttpOnly
                secure           // Secure only on HTTPS
        ));
    }

    @GET
    @Path("/")   // endpoint = /realms/{realm}/force-logout?redirect_uri=...
    public Response forceLogout(@QueryParam("redirect_uri") String redirectUri) {
        LOG.infof("Force-logout initiated; redirect_uri=%s", redirectUri);
        final KeycloakContext ctx = session.getContext();
        final RealmModel realm = ctx.getRealm();
        final ClientConnection connection = ctx.getConnection();
        final HttpRequest request = ctx.getHttpRequest();

        // HTTPS detection: on staging this will be "https"
        final boolean isHttps = "https".equalsIgnoreCase(ctx.getUri().getBaseUri().getScheme());

        EventBuilder event = new EventBuilder(realm, session, connection).event(EventType.LOGOUT);

        try {
            // 1) If an SSO session exists, log it out server-side (clients + session)
            AuthenticationManager.AuthResult auth = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
            if (auth != null) {
                UserSessionModel userSession = auth.getSession();
                AuthenticationManager.backchannelLogout(
                        session, realm, userSession, ctx.getUri(), connection, ctx.getRequestHeaders(), true);
                event.user(userSession.getUser()).session(userSession).success();
            }

            // 2) Expire Keycloak-managed SSO cookies
            AuthenticationManager.expireIdentityCookie(session);
            AuthenticationManager.expireAuthSessionCookie(session);
            AuthenticationManager.expireRememberMeCookie(session);

            // 3) Aggressively expire transient + legacy cookies on all relevant paths
            String realmPathNoSlash   = "/realms/" + realm.getName();
            String realmPathWithSlash = realmPathNoSlash + "/";

            Response.ResponseBuilder rb = Response.ok();

            // AUTH_SESSION_ID & KC_RESTART (the ones that trigger the banner)
            for (String p : new String[]{realmPathNoSlash, realmPathWithSlash, "/"}) {
                addExpireCookie(rb, "AUTH_SESSION_ID", p, isHttps);
                addExpireCookie(rb, "KC_RESTART",     p, isHttps);
            }

            // Identity/session + legacy + remember-me (belt & suspenders)
            String[] names = {
                    "KEYCLOAK_IDENTITY", "KEYCLOAK_IDENTITY_LEGACY",
                    "KEYCLOAK_SESSION",  "KEYCLOAK_SESSION_LEGACY",
                    "KEYCLOAK_REMEMBER_ME"
            };
            for (String n : names) {
                addExpireCookie(rb, n, realmPathWithSlash, isHttps);
                addExpireCookie(rb, n, "/",               isHttps);
            }

            // 4) Remove server-side transient authentication session
            AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
            var authSession = ctx.getAuthenticationSession();
            if (authSession != null) {
                asm.removeAuthenticationSession(authSession.getRealm(), authSession, true);
            } else {
                SystemClientUtil.checkSkipLink(session, null);
            }

            // 5) Redirect back to app (recommended) or return 204
            if (redirectUri != null && !redirectUri.isBlank()) {
                rb.status(Response.Status.FOUND).location(java.net.URI.create(redirectUri));
            } else {
                rb.status(Response.Status.NO_CONTENT);
            }

            return rb.build();

        } catch (Exception e) {
            LOG.error("Force-logout failed", e);
            return Response.serverError().entity("force-logout failed: " + e.getMessage()).build();
        }
    }
}
