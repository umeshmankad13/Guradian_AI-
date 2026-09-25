const SUSPICIOUS_TLDS = [
  "zip", "top", "xyz", "tk", "ml", "ga", "cf", "info",
  "click", "gq", "cab", "vip", "work", "loan"
];

const PHISHING_KEYWORDS = [
  "login", "verify", "secure", "account", "update", "confirm",
  "signin", "banking", "crypto", "wallet", "auth", "password", "credential"
];

const BRAND_WORDS = [
  "paypal", "google", "goggle", "microsoft", "apple",
  "amazon", "facebook", "chase", "bankofamerica", "instagram"
];

function severityWeight(sev) {
  return { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 }[sev] || 1;
}

function indicator(code, title, detail, severity, weight) {
  return { code, title, detail, severity, weight };
}

function analyzeUrl(urlInput, pageSignals = {}) {
  if (!urlInput || typeof urlInput !== "string") {
    throw new Error("INVALID_URL");
  }

  let urlObj;
  try {
    urlObj = new URL(urlInput);
  } catch {
    throw new Error("INVALID_URL");
  }

  const raw = urlInput;
  const indicators = [];
  const hostname = urlObj.hostname || "";
  const protocol = urlObj.protocol.replace(":", "");
  const port = urlObj.port;
  const pathname = urlObj.pathname || "";
  const hostParts = hostname.split(".").filter(Boolean); // > 0

  // 1. Dangerous scheme
  const scheme = (urlObj.protocol || "").toLowerCase();
  if (scheme.startsWith("javascript") || scheme.startsWith("data") || scheme.startsWith("vbscript")) {
    indicators.push(indicator("DANGEROUS_SCHEME", "Dangerous URL scheme", `${scheme} requires code execution`, "CRITICAL", 40));
  }

  // 2. IP address domain
  const ipv4 = /^(\d{1,3}\.){3}\d{1,3}$/;
  const ipv6 = /^[0-9a-f]{1,4}(:[0-9a-f]{1,4}){7}$/i;
  if (ipv4.test(hostname) || ipv6.test(hostname)) {
    indicators.push(indicator(
      "IP_ADDRESS", "IP address domain",
      `Domain is a raw IP (${hostname}) instead of a domain name`,
      "HIGH", 45
    ));
  }

  // 3. Punycode / unicode domain
  if (hostname.startsWith("xn--") || /[\u00C0-\u024F\u0370-\u1DFF\u2E80-\uA4CF\uF900-\uFAFF\uFF00-\uFFEF]/) {
    indicators.push(indicator(
      "PUNYCODE_DOMAIN", "Internationalized / punycode domain",
      "The domain may be obfuscated with unicode characters",
      "HIGH", 30
    ));
  }

  // 4. Unusual port
  if (port && ![80, 443, 8080].includes(port)) {
    indicators.push(indicator(
      "UNUSUAL_PORT", "Non-standard port",
      `Port ${port} is not commonly used for public sites`,
      "MEDIUM", 20
    ));
  }

  // 5. Insecure HTTP
  if (protocol === "http") {
    indicators.push(indicator(
      "INSECURE_HTTP", "Plain HTTP transport",
      "Connection is not encrypted with HTTPS",
      "MEDIUM", 25
    ));
  }

  // 6. Subdomain anomalies
  if (hostParts.length <= 2) {
    indicators.push(indicator(
      "MISSING_SUBDOMAINS", "Very short hostname",
      `Host has ${hostParts.length} part(s); bare domains can be temporary`,
      "LOW", 5
    ));
  } else if (hostParts.length > 4) {
    indicators.push(indicator(
      "EXTRA_SUBDOMAINS", "Excessive subdomains",
      `Host has ${hostParts.length} dot-separated parts, possibly cloaking`,
      "MEDIUM", 15
    ));
  }

  // 7. Obfuscation: many @
  if ((raw.match(/@/g) || []).length > 1) {
    indicators.push(indicator(
      "OBFUSCATION_AT", "Multiple '@' signs",
      "Extra '@' can hide the real destination",
      "HIGH", 30
    ));
  }

  // 8. Long token in path / query
  const segments = [...(pathname + (urlObj.search || "")).split("/")];
  const longSegment = segments.find((s) => s.length > 40);
  if (longSegment) {
    indicators.push(indicator(
      "OBFUSCATION_LONG_TOKEN", "Long path/query token",
      `A ${longSegment.length}-char segment looks machine-generated`,
      "LOW", 10
    ));
  }

  // 9. Base64-looking tokens
  if (/[A-Za-z0-9+/]{40,}={0,2}/.test(pathname + (urlObj.search || ""))) {
    indicators.push(indicator(
      "OBFUSCATION_BASE64", "Base64-looking payload",
      "Encoded data in the path can hide malicious params",
      "MEDIUM", 15
    ));
  }

  // 10. Many dots
  if (hostname.split(".").length > 6 || (pathname.match(/\./g) || []).length > 10) {
    indicators.push(indicator(
      "OBFUSCATION_MANY_DOTS", "Excessive dot separators",
      "Many dots can cloak the real domain",
      "LOW", 5
    ));
  }

  // 11. Many slashes
  const afterAuthority = urlObj.pathname || "";
  if (afterAuthority.includes("//")) {
    indicators.push(indicator(
      "OBFUSCATION_MANY_SLASHES", "Repeated slashes in path",
      "Double slashes can mimic trusted path structures",
      "LOW", 5
    ));
  }

  // 12. Hex escapes
  if (/%[0-9a-fA-F]{2}/.test(raw)) {
    indicators.push(indicator(
      "OBFUSCATION_HEX_ESCAPE", "Hex-escaped characters",
      "Encoded characters may disguise parameters",
      "LOW", 5
    ));
  }

  // 13. Suspicious TLD
  const tld = hostParts[hostParts.length - 1]?.toLowerCase();
  if (tld && SUSPICIOUS_TLDS.includes(tld)) {
    indicators.push(indicator(
      "SUSPICIOUS_TLD", "Suspicious top-level domain",
      `TLD ".${tld}" is frequently abused by phishers`,
      "HIGH", 35
    ));
  }

  // 14. Phishing keyword
  const combinedHost = (hostname + pathname).toLowerCase();
  const foundPhish = PHISHING_KEYWORDS.filter((k) => /(^|[-._~]?)/.test(k) && combinedHost.includes(k));
  if (foundPhish.length) {
    indicators.push(indicator(
      "PHISHING_KEYWORD", "Phishing-style keywords",
      `Found "${foundPhish.join(", ")}" in host or path`,
      "HIGH", 25
    ));
  }

  // 15. Brand impersonation
  const brandFound = BRAND_WORDS.find((b) => combinedHost.includes(b));
  if (brandFound) {
    indicators.push(indicator(
      "BRAND_IMPERSONATION", "Possible brand impersonation",
      `Host/path mention "${brandFound}" inside a non-official domain pattern`,
      "HIGH", 30
    ));
  }

  // 16. Page signals
  if (pageSignals.hasPasswordField) {
    indicators.push(indicator(
      "PAGE_PASSWORD_INPUT", "Password input detected",
      "The page contains a password field — a common harvest target",
      "MEDIUM", 20
    ));
  }
  if (pageSignals.insecureForms) {
    indicators.push(indicator(
      "PAGE_INSECURE_FORM", "Insecure form submission",
      "The page submits a form over plain HTTP",
      "HIGH", 30
    ));
  }
  if (pageSignals.iframeCount && pageSignals.iframeCount > 0) {
    indicators.push(indicator(
      "PAGE_HIDDEN_IFRAME", "Embedded iframe",
      `${pageSignals.iframeCount} hidden iframe(s) are embedded`,
      "MEDIUM", 15
    ));
  }

  return {
    url: urlInput,
    hostname,
    protocol,
    port: port || null,
    pathname,
    indicators,
  };
}

module.exports = { analyzeUrl };