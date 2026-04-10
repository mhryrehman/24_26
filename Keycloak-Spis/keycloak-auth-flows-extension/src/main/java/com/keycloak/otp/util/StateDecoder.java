package com.keycloak.otp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class StateDecoder {

    private static final ObjectMapper OM = new ObjectMapper();

    private StateDecoder() {
    }

    /**
     * Try to decode like the frontend: base64 → url-decode → json. Returns JsonNode or null.
     */
    public static JsonNode decodeState(String state) {
        if (state == null || state.isBlank()) return null;

        // 1) Try: (maybe) base64, then optional URL decode, then JSON
        // Attempt both URL-safe and standard Base64
        String[] candidates = new String[]{
                tryBase64(state, true),
                tryBase64(state, false),
                // If not base64, consider the original as a candidate text
                state
        };

        for (String candidate : candidates) {
            if (candidate == null) continue;

            // Try JSON directly
            JsonNode j = tryJson(candidate);
            if (j != null) return j;

            // Try URL-decode, then JSON
            String urlDecoded = tryUrlDecode(candidate);
            if (urlDecoded != null) {
                j = tryJson(urlDecoded);
                if (j != null) return j;
            }
        }
        return null;
    }

    /**
     * Extract a string field from a decoded state JSON (returns null if absent/not a string).
     */
    public static String getString(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v != null && v.isTextual()) ? v.asText() : null;
    }

    // --- helpers ---

    private static JsonNode tryJson(String txt) {
        try {
            return OM.readTree(txt);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String tryUrlDecode(String txt) {
        try {
            return URLDecoder.decode(txt, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String tryBase64(String txt, boolean urlSafe) {
        try {
            String normalized = normalizeBase64(txt, urlSafe);
            byte[] bytes = (urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder()).decode(normalized);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Add padding and swap chars for URL-safe variant if needed.
     */
    private static String normalizeBase64(String s, boolean urlSafe) {
        String t = s.trim();
        if (urlSafe) {
            // Convert standard to URL-safe if it looks mixed
            t = t.replace('+', '-').replace('/', '_');
        }
        // add padding if missing
        int mod = t.length() % 4;
        if (mod != 0) {
            t += "=".repeat(4 - mod);
        }
        return t;
    }
}

