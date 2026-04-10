package com.keycloak.security.extensions.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class ExternalUserAccessService {

    private static final Logger LOG = Logger.getLogger(ExternalUserAccessService.class);

    private ExternalUserAccessService() {}

    // Per (tokenUrl||clientId) token cache
    private static final ConcurrentHashMap<String, TokenHolder> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static String cacheKey(String tokenUrl, String clientId) { return tokenUrl + "||" + clientId; }

    public record ApiResult(int status, JsonNode body) {}

    /**
     * Calls the external API with a cached client-credentials token.
     * If the API returns 401, refreshes the client token and retries once.
     */
    public static ApiResult fetchUserAccess(KeycloakSession session,
                                            String apiBase,
                                            String identifier,
                                            String userType,
                                            String tokenUrl,
                                            String clientId,
                                            String clientSecret,
                                            int timeoutMs,
                                            int skewSec) throws Exception {

        String bearer = getClientToken(session, tokenUrl, clientId, clientSecret, timeoutMs, skewSec);

        ApiResult res = callUserAccessApi(session, apiBase, identifier, userType, bearer, timeoutMs);
        LOG.info("User-access API returned status " + res.status);
        if (res.status == 401) {
            // invalidate & refresh
            invalidate(tokenUrl, clientId);
            bearer = getClientToken(session, tokenUrl, clientId, clientSecret, timeoutMs, skewSec);
            res = callUserAccessApi(session, apiBase, identifier, userType, bearer, timeoutMs);
            LOG.info("After token refresh, user-access API returned status " + res.status);
        }
        return res;
    }

    // ---- HTTP ----

    private static ApiResult callUserAccessApi(KeycloakSession session,
                                               String apiBase,
                                               String identifier,
                                               String userType,
                                               String bearer,
                                               int timeoutMs) throws Exception {
        LOG.info("Calling user-access API at " + apiBase + " for identifier " + identifier + " and userType " + userType);
        SimpleHttp req = SimpleHttp.doGet(apiBase, session)
                .param("identifier", identifier)
                .param("userType", userType)
                .header("Authorization", "Bearer " + bearer)
                .header("Accept", "application/json")
                .connectTimeoutMillis(timeoutMs);

        SimpleHttp.Response resp = req.asResponse();
        int status = resp.getStatus();
        if (status == 200) {
            return new ApiResult(200, resp.asJson());
        }
        if (status == 401) {
            return new ApiResult(401, null);
        }
        // Non-2xx → return status without body
        if (LOG.isDebugEnabled()) {
            try { LOG.debugf("User-access API non-2xx: %d, body: %s", status, resp.asString()); }
            catch (Exception ignore) {}
        }
        return new ApiResult(status, null);
    }

    // ---- Token cache ----

    private static String getClientToken(KeycloakSession session,
                                         String tokenUrl,
                                         String clientId,
                                         String clientSecret,
                                         int timeoutMs,
                                         int skewSec) throws Exception {
        LOG.info("Getting client token for " + clientId + " at " + tokenUrl);
        String key = cacheKey(tokenUrl, clientId);
        TokenHolder holder = TOKEN_CACHE.computeIfAbsent(key, k -> new TokenHolder());
        String cached = holder.getIfValid(skewSec);
        if (cached != null) return cached;

        return holder.refreshIfNeeded(() -> fetchClientToken(session, tokenUrl, clientId, clientSecret, timeoutMs), skewSec);
    }

    private static void invalidate(String tokenUrl, String clientId) {
        TokenHolder h = TOKEN_CACHE.get(cacheKey(tokenUrl, clientId));
        if (h != null) h.invalidate();
    }

    private static TokenResponse fetchClientToken(KeycloakSession session,
                                                  String tokenUrl,
                                                  String clientId,
                                                  String clientSecret,
                                                  int timeoutMs) throws Exception {
        LOG.info("Fetching new client token for " + clientId + " at " + tokenUrl);
        JsonNode json = SimpleHttp.doPost(tokenUrl, session)
                .param("grant_type", "client_credentials")
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .header("Accept", "application/json")
                .connectTimeoutMillis(timeoutMs)
                .asJson();

        String accessToken = (json.hasNonNull("access_token") ? json.get("access_token").asText() : null);
        int expiresIn = (json.hasNonNull("expires_in") ? json.get("expires_in").asInt() : 300);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("No access_token in client token response");
        }
        return new TokenResponse(accessToken, Instant.now().plusSeconds(expiresIn));
    }

    // ---- Holder classes ----

    private static class TokenHolder {
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicReference<String> token = new AtomicReference<>(null);
        private final AtomicReference<Instant> expiry = new AtomicReference<>(Instant.EPOCH);

        String getIfValid(int skewSec) {
            String t = token.get();
            if (t == null) return null;
            Instant exp = expiry.get();
            LOG.info("Token expires at " + exp);
            return (Instant.now().isBefore(exp.minusSeconds(skewSec))) ? t : null;
        }

        String refreshIfNeeded(Callable<TokenResponse> fetcher, int skewSec) throws Exception {
            LOG.info("Refreshing client token if needed…");
            // fast path
            String t = getIfValid(skewSec);
            LOG.info("After fast-path check, token is " + (t == null ? "null/expired" : "valid"));
            if (t != null) return t;

            // IMPORTANT: fetch OUTSIDE the lock
            TokenResponse fetched = fetcher.call();

            // now acquire lock briefly just to set if still needed
            lock.lock();
            try {
                t = getIfValid(skewSec);
                if (t != null) return t;
                token.set(fetched.accessToken());
                expiry.set(fetched.expiresAt());
                return fetched.accessToken();
            } finally {
                lock.unlock();
            }
        }

        void invalidate() {
            token.set(null);
            expiry.set(Instant.EPOCH);
        }
    }

    private record TokenResponse(String accessToken, Instant expiresAt) {}
}
