package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.ScanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    public record Result(AnalysisOutcome outcome, boolean cached) {}

    private final VerdictCacheService cache;

    public AnalysisService(VerdictCacheService cache) {
        this.cache = cache;
    }

    public Result analyze(ScanRequest request) {
        String key = cache.fingerprint(request);

        Optional<AnalysisOutcome> hit = cache.get(key);
        if (hit.isPresent()) {
            return new Result(hit.get(), true);
        }

        AnalysisOutcome outcome = runRules(request);   // TODO: replace with the Analysis Orchestrator call
        cache.put(key, outcome);
        return new Result(outcome, false);
    }

    private AnalysisOutcome runRules(ScanRequest request) {
        String domain = UrlUtils.extractDomain(request.url());
        int score = 100;
        List<String> findings = new ArrayList<>();
        List<AnalysisOutcome.IndicatorData> indicators = new ArrayList<>();
        List<AnalysisOutcome.ElementData> elements = new ArrayList<>();

        boolean https = request.url().startsWith("https://");
        indicators.add(new AnalysisOutcome.IndicatorData("URL", "uses_https", String.valueOf(https), !https));
        if (https) {
            findings.add("Connection uses HTTPS");
        } else {
            score -= 20;
            findings.add("Connection does not use HTTPS");
        }

        if (request.tls() != null) {
            boolean valid = request.tls().valid();
            indicators.add(new AnalysisOutcome.IndicatorData("TLS", "tls_valid", String.valueOf(valid), !valid));
            if (valid) {
                findings.add("TLS certificate is valid");
            } else {
                score -= 30;
                findings.add("TLS certificate is invalid");
            }
        }

        if (request.redirects() != null) {
            int count = request.redirects().size();
            boolean tooMany = count > 3;
            indicators.add(new AnalysisOutcome.IndicatorData("REDIRECT", "redirect_count", String.valueOf(count), tooMany));
            if (tooMany) {
                score -= 10;
                findings.add("Many redirects detected (" + count + ")");
            } else {
                findings.add("No suspicious redirects");
            }
        }

        if (request.page() != null) {
            int passwordFields = request.page().passwordFields();
            boolean riskyPassword = passwordFields > 0 && !https;
            elements.add(new AnalysisOutcome.ElementData("PASSWORD_FIELD", String.valueOf(passwordFields),
                    riskyPassword, riskyPassword ? "Password field on a page without HTTPS" : null));
            if (riskyPassword) {
                score -= 20;
                findings.add("Password field on an unencrypted page");
            }

            int scripts = request.page().externalScripts();
            boolean manyScripts = scripts > 10;
            elements.add(new AnalysisOutcome.ElementData("EXTERNAL_SCRIPT", String.valueOf(scripts),
                    manyScripts, manyScripts ? "Unusually many external scripts" : null));
            if (manyScripts) {
                score -= 10;
                findings.add("Many external scripts (" + scripts + ")");
            }
        }

        score = Math.max(score, 0);
        String level = score >= 80 ? "LOW" : score >= 50 ? "MEDIUM" : "HIGH";
        List<String> recommendations = switch (level) {
            case "LOW" -> List.of("No action needed");
            case "MEDIUM" -> List.of("Avoid entering personal information on this site");
            default -> List.of("Do not enter credentials or personal data", "Leave this site");
        };
        String explanation = "Placeholder rule-based analysis of " + domain
                + " gave a security score of " + score + "/100.";

        return new AnalysisOutcome(score, level, 0.60, findings, explanation, recommendations, indicators, elements);
    }
}