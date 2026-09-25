const { knowledgeBase } = require("../data/knowledgeBase");

const knowledgeByRule = new Map(
    knowledgeBase.map((entry) => [entry.rule, entry])
);

const SEVERITY_LABELS = new Set(["LOW", "MEDIUM", "HIGH", "CRITICAL"]);

function normalizeSeverity(severity) {
    const normalized = String(severity || "LOW").toUpperCase();
    return SEVERITY_LABELS.has(normalized) ? normalized : "LOW";
}

function generate({ url, indicators = [], riskScore = 0, severity = "LOW" } = {}) {
    const safeIndicators = Array.isArray(indicators) ? indicators : [];
    const normalizedSeverity = normalizeSeverity(severity);
    const score = Number.isFinite(Number(riskScore)) ? Number(riskScore) : 0;
    const findings = safeIndicators.map((indicator) => {
        const knowledge = knowledgeByRule.get(indicator.code);

        return {
            code: indicator.code,
            title: indicator.title,
            detail: indicator.detail,
            severity: normalizeSeverity(indicator.severity),
            summary: knowledge?.summary || indicator.detail,
            mitigation: knowledge?.mitigation || "Treat this signal cautiously and verify the site independently.",
            recommendedAction: knowledge?.recommendedAction || "Do not enter sensitive information until the site is verified.",
        };
    });

    if (findings.length === 0) {
        const safeKnowledge = knowledgeByRule.get("SAFE");
        findings.push({
            code: "SAFE",
            title: "No suspicious signals detected",
            detail: safeKnowledge.summary,
            severity: "LOW",
            summary: safeKnowledge.summary,
            mitigation: safeKnowledge.mitigation,
            recommendedAction: safeKnowledge.recommendedAction,
        });
    }

    const hostname = getHostname(url);
    const summary = buildSummary(normalizedSeverity, score, hostname);
    const reasoningSteps = [
        `Analyzed ${hostname || "the submitted URL"} using local URL and page heuristics.`,
        `Detected ${safeIndicators.length} suspicious signal${safeIndicators.length === 1 ? "" : "s"}.`,
        `Combined signal weights produced a risk score of ${score}/100 (${normalizedSeverity}).`,
    ];

    return {
        summary,
        explanation: buildExplanation(findings, normalizedSeverity),
        reasoningSteps,
        findings,
    };
}

function getHostname(url) {
    try {
        return new URL(url).hostname;
    } catch {
        return "";
    }
}

function buildSummary(severity, score, hostname) {
    const target = hostname ? ` for ${hostname}` : "";
    if (severity === "CRITICAL") return `Critical risk${target}: this site should not be trusted (${score}/100).`;
    if (severity === "HIGH") return `High risk${target}: several phishing or security signals were detected (${score}/100).`;
    if (severity === "MEDIUM") return `Medium risk${target}: review the detected security signals before continuing (${score}/100).`;
    return `Low risk${target}: no strong warning signals were detected (${score}/100).`;
}

function buildExplanation(findings, severity) {
    if (severity === "LOW" && findings.length === 1 && findings[0].code === "SAFE") {
        return findings[0].mitigation;
    }

    const lead = `The analysis classified this URL as ${severity.toLowerCase()} risk based on ${findings.length} finding${findings.length === 1 ? "" : "s"}.`;
    const details = findings
        .slice(0, 3)
        .map((finding) => `${finding.title}: ${finding.summary}`)
        .join(" ");
    const remainder = findings.length > 3 ? ` ${findings.length - 3} additional finding${findings.length - 3 === 1 ? "" : "s"} were detected.` : "";
    return `${lead} ${details}${remainder}`;
}

module.exports = { generate };
