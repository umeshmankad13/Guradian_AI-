// Offline RAG-style cybersecurity knowledge base.
// The "AI" layer retrieves matching entries by indicator rule code and
// synthesizes plain-language guidance, entirely without external APIs.

const knowledgeBase = [
  {
    id: "kb_ip_address",
    rule: "IP_ADDRESS",
    category: "URL anomaly",
    severity: "HIGH",
    summary: "The domain is a raw IP address instead of a real domain name.",
    mitigation:
      "Legitimate sites almost never use raw IP addresses as login targets.",
    recommendedAction: "Do not enter credentials. Visit the official domain directly.",
  },
  {
    id: "kb_punycode",
    rule: "PUNYCODE_DOMAIN",
    category: "URL anomaly",
    severity: "HIGH",
    summary: "The hostname uses punycode/unicode, a common trick to spoof a trusted name.",
    mitigation:
      "Unicode lookalike domains are frequently used in phishing to impersonate brands.",
    recommendedAction: "Compare the displayed characters carefully and type the site address manually.",
  },
  {
    id: "kb_unusual_port",
    rule: "UNUSUAL_PORT",
    category: "URL anomaly",
    severity: "MEDIUM",
    summary: "The URL uses a non-standard port, which is unusual for public sites.",
    mitigation: "Attacks sometimes host lookalike services on unusual ports.",
    recommendedAction: "Verify that the official site really uses this port.",
  },
  {
    id: "kb_http",
    rule: "INSECURE_HTTP",
    category: "Transport",
    severity: "MEDIUM",
    summary: "The page is served over plain HTTP instead of encrypted HTTPS.",
    mitigation:
      "Without TLS, login data and any submitted content can be read or tampered with.",
    recommendedAction: "Do not enter sensitive data on an HTTP page.",
  },
  {
    id: "kb_tld",
    rule: "SUSPICIOUS_TLD",
    category: "Domain reputation",
    severity: "HIGH",
    summary: "The top-level domain is frequently abused by phishing campaigns.",
    mitigation:
      "TLDs like .xyz, .top, .zip, .tk and .ml are often used for malicious or temporary domains.",
    recommendedAction: "Treat this site with caution and avoid login/sensitive actions.",
  },
  {
    id: "kb_phishing",
    rule: "PHISHING_KEYWORD",
    category: "Phishing signal",
    severity: "HIGH",
    summary: "The URL contains words commonly used in phishing (login, verify, secure...).",
    mitigation:
      "These keywords are heavily abused to make fake pages look like account or security alerts.",
    recommendedAction: "Open the official login page from your bookmarks or a search engine.",
  },
  {
    id: "kb_brand",
    rule: "BRAND_IMPERSONATION",
    category: "Phishing signal",
    severity: "HIGH",
    summary: "The address appears to impersonate a well-known brand.",
    mitigation:
      "Brand lookalikes with injected words or typos are a classic phishing pattern.",
    recommendedAction: "Navigate manually to the brand's real website.",
  },
  {
    id: "kb_at",
    rule: "OBFUSCATION_AT",
    category: "Obfuscation",
    severity: "HIGH",
    summary: "The URL contains multiple '@' characters, a known browser parsing trick.",
    mitigation:
      "Content after '@' can hide the real destination from some users.",
    recommendedAction: "Do not open links with extra @ characters.",
  },
  {
    id: "kb_base64",
    rule: "OBFUSCATION_BASE64",
    category: "Obfuscation",
    severity: "MEDIUM",
    summary: "The URL contains base64-looking encoded data.",
    mitigation:
      "Encoded payloads are often used to hide malicious parameters.",
    recommendedAction: "Do not proceed; encoded parameters may redirect or attack the browser.",
  },
  {
    id: "kb_token",
    rule: "OBFUSCATION_LONG_TOKEN",
    category: "Obfuscation",
    severity: "LOW",
    summary: "The URL contains an unusually long token in its path or query.",
    mitigation:
      "Tracking/analytics tokens are common, but overly long path tokens can indicate obfuscation.",
    recommendedAction: "Be cautious if the token appears unrelated to a trusted pattern.",
  },
  {
    id: "kb_dots",
    rule: "OBFUSCATION_MANY_DOTS",
    category: "Obfuscation",
    severity: "LOW",
    summary: "The URL contains an unusual number of dot-separated segments.",
    mitigation:
      "Many dots can be used to confuse the real domain.",
    recommendedAction: "Read the domain carefully from right to left.",
  },
  {
    id: "kb_slashes",
    rule: "OBFUSCATION_MANY_SLASHES",
    category: "Obfuscation",
    severity: "LOW",
    summary: "The URL contains repeated slash sequences in the path.",
    mitigation:
      "Multiple slashes can hide redirects or mimic trusted path structures.",
    recommendedAction: "Inspect the full path before continuing.",
  },
  {
    id: "kb_hex",
    rule: "OBFUSCATION_HEX_ESCAPE",
    category: "Obfuscation",
    severity: "LOW",
    summary: "The URL contains hex-escaped characters.",
    mitigation:
      "Encoded characters can be used to disguise malicious parameters.",
    recommendedAction: "Decode the URL cautiously or avoid it.",
  },
  {
    id: "kb_scheme",
    rule: "DANGEROUS_SCHEME",
    category: "Danger",
    severity: "CRITICAL",
    summary: "The URL uses a dangerous URI scheme.",
    mitigation:
      "javascript:, data: or vbscript: schemes can execute code or load malicious content.",
    recommendedAction: "Do not open concentrations of such links.",
  },
  {
    id: "kb_password_input",
    rule: "PAGE_PASSWORD_INPUT",
    category: "Page signal",
    severity: "MEDIUM",
    summary: "The page contains a password input field.",
    mitigation:
      "Password fields are what phishers harvest. Only enter secrets on trusted pages.",
    recommendedAction: "Confirm HTTPS and the exact domain before typing a password.",
  },
  {
    id: "kb_insecure_form",
    rule: "PAGE_INSECURE_FORM",
    category: "Page signal",
    severity: "HIGH",
    summary: "The page contains a form that posts over plain HTTP.",
    mitigation:
      "Credentials submitted over HTTP can be intercepted in transit.",
    recommendedAction: "Do not submit any data on this page.",
  },
  {
    id: "kb_iframe",
    rule: "PAGE_HIDDEN_IFRAME",
    category: "Page signal",
    severity: "MEDIUM",
    summary: "The page embeds a hidden iframe.",
    mitigation:
      "Hidden iframes can load injected or malicious content silently.",
    recommendedAction: "Inspect the page if you selected to continue.",
  },
  {
    id: "kb_safe",
    rule: "SAFE",
    category: "Clean",
    severity: "LOW",
    summary: "No suspicious URL or page signals were detected.",
    mitigation:
      "This does not guarantee the site is legitimate — only that no heuristic signals fired.",
    recommendedAction: "Use due diligence, check the domain, and confirm HTTPS.",
  },
];

module.exports = { knowledgeBase };