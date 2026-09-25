const express = require("express");
const cors = require("cors");
const analyzer = require("./analyzer");
const riskEngine = require("./risk-engine");
const aiService = require("./ai-service");

const app = express();
const PORT = 5000;

app.use(cors());
app.use(express.json({ limit: "10kb" }));

const scanHistory = [];
let requestId = 0;

app.use((req, res, next) => {
  const start = Date.now();
  res.on("finish", () => {
    const ms = Date.now() - start;
    console.log(`${req.method} ${req.url} ${res.statusCode} ${ms}ms`);
  });
  next();
});

app.get("/", (req, res) => {
  res.json({ status: "ok", service: "GuardianAI API", version: "1.0.0" });
});

app.post("/analyze", (req, res) => {
  const { url, pageSignals } = req.body || {};
  try {
    const parsed = analyzer.analyzeUrl(url, pageSignals || {});
    const risk = riskEngine.evaluate(parsed.indicators, url);
    const explanation = aiService.generate({
      url,
      indicators: parsed.indicators,
      riskScore: risk.score,
      severity: risk.severity,
    });

    const result = {
      url,
      score: risk.score,
      severity: risk.severity,
      summary: explanation.summary,
      explanation: explanation.explanation,
      reasoning: explanation.reasoningSteps,
      findings: explanation.findings,
      recommendations: risk.recommendations,
      timestamp: new Date().toISOString(),
    };

    scanHistory.push(result);
    if (scanHistory.length > 50) scanHistory.shift();

    res.json(result);
  } catch (err) {
    if (err.message === "INVALID_URL") {
      return res.status(400).json({ error: "Invalid or missing URL" });
    }
    console.error(err);
    res.status(500).json({ error: "Internal analysis error" });
  }
});

app.get("/history", (req, res) => {
  res.json({ history: scanHistory.slice(-50) });
});

app.use((req, res) => {
  res.status(404).json({ error: "Route not found" });
});

app.listen(PORT, () => {
  console.log(`GuardianAI API listening on http://localhost:${PORT}`);
});

module.exports = app;