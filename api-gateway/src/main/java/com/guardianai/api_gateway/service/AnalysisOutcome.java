package com.guardianai.api_gateway.service;

import java.util.List;

public record AnalysisOutcome(
        int score,
        String level,
        double confidence,
        List<String> findings,
        String explanation,
        List<String> recommendations,
        List<AnalysisOutcome.IndicatorData> indicators,
        List<AnalysisOutcome.ElementData> elements
) {
    public record IndicatorData(String type, String name, String value, boolean malicious) {}

    public record ElementData(String type, String value, boolean suspicious, String reason) {}
}