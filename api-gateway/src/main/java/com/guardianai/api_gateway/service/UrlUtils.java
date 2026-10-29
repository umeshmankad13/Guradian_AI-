package com.guardianai.api_gateway.service;

import java.net.URI;
import java.util.Locale;

final class UrlUtils {

    private UrlUtils() {}

    static String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : "unknown";
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
    }

    /** Lowercases scheme and host and drops the #fragment, so cosmetic differences share one cache entry. */
    static String normalize(String raw) {
        String trimmed = raw.trim();
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() == null) {
                return trimmed;     // unusual URL: don't risk two different hosts sharing a key
            }
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + port + path + query;
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }
}