const WEIGHT_TABLE = {
  DANGEROUS_SCHEME: 40,
  IP_ADDRESS: 45,
  PUNYCODE_DOMAIN: 30,
  UNUSUAL_PORT: 20,
  INSECURE_HTTP: 25,
  MISSING_SUBDOMAINS: 5,
  EXTRA_SUBDOMAINS: 15,
  OBFUSCATION_AT: 30,
  OBFUSCATION_LONG_TOKEN: 10,
  OBFUSCATION_BASE64: 15,
  OBFUSCATION_MANY_DOTS: 5,
  OBFUSCATION_MANY_SLASHES: 5,
  OBFUSCATION_HEX_ESCAPE: 5,
  SUSPICIOUS_TLD: 35,
  PHISHING_KEYWORD: 25,
  BRAND_IMPERSONATION: 30,
  PAGE_PASSWORD_INPUT: 20,
  PAGE_INSECURE_FORM: 30,
  PAGE_HIDDEN_IFRAME: 15,
};

const MAX_POSSIBLE = Object.values(WEIGHT_TABLE).reduce((a, b) => a + b, 0);
const SEVERITY_MULT = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };

function getDecision(score, severity, indicatorCodes = []) {
  const criticalIndicators = new Set([
    "DANGEROUS_SCHEME",
    "IP_ADDRESS",
    "PUNYCODE_DOMAIN",
    "BRAND_IMPERSONATION",
    "PAGE_INSECURE_FORM",
    "PHISHING_KEYWORD",
    "SUSPICIOUS_TLD",
  ]);

  const hasCriticalIndicator = indicatorCodes.some((code) => criticalIndicators.has(code));

  if (severity === "CRITICAL" || score >= 80 || hasCriticalIndicator) {
    return {
      action: "BLOCK",
      level: "CRITICAL",
      reason: "Critical threat indicators detected; website should not be opened.",
      blockPage: true,
    };
  }

  if (severity === "HIGH" || score >= 60) {
    return {
      action: "BLOCK",
      level: "HIGH",
      reason: "High vulnerability detected; access is blocked to prevent risk.",
      blockPage: true,
    };
  }

  if (severity === "MEDIUM" || score >= 35) {
    return {
      action: "WARN",
      level: "MEDIUM",
      reason: "Suspicious signals detected; user should be warned before continuing.",
      blockPage: false,
    };
  }

  return {
    action: "ALLOW",
    level: "LOW",
    reason: "No dangerous signals beyond the normal threshold.",
    blockPage: false,
  };
}

function evaluate(indicators, url) {
  let weighted = 0;
  for (const ind of indicators || []) {
    const base = WEIGHT_TABLE[ind.code] || 5;
    const mult = SEVERITY_MULT[ind.severity] || 1;
    weighted += base * mult;
  }

  let score = Math.round((weighted / MAX_POSSIBLE) * 100);
  score = Math.min(100, Math.max(0, score));

  let severity = "LOW";
  if (score >= 85) severity = "CRITICAL";
  else if (score >= 65) severity = "HIGH";
  else if (score >= 40) severity = "MEDIUM";

  const indicatorCodes = (indicators || []).map((ind) => ind.code);
  const decision = getDecision(score, severity, indicatorCodes);
  const hints = (indicators || []).map((ind) => `${ind.title}: ${ind.detail}`);

  const recommendations = [];
  if (severity !== "LOW") recommendations.push("Do not enter sensitive data on this site.");
  if (decision.action === "BLOCK") recommendations.push("Close this page and navigate to the official domain directly.");
  if (hints.some((h) => h.includes("HTTP"))) recommendations.push("Enable HTTPS (Look for a lock icon in the address bar).");
  if (hints.some((h) => h.includes("password") || h.includes("Password"))) recommendations.push("Never reuse your master password here.");
  if (severity === "LOW") recommendations.push("Perform usual caution: verify padlock and domain.");
  recommendations.push("Use a jailer/secondary service to confirm the site.");

  return {
    score,
    severity,
    decision,
    hints,
    recommendations,
    url,
    vulnerabilityLevel: decision.level,
    blocked: decision.blockPage,
  };
}
module.exports = { evaluate, getDecision }