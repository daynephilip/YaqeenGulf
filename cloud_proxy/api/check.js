import 'dotenv/config';
import { validateSsrfUrl, ssrfSafeFetch } from '../lib/ssrf.js';
import { checkDailyKillSwitch, checkIpRateLimit } from '../lib/limiter.js';
import { extractArticleText } from '../lib/extractor.js';
import { buildDelimitedPrompt, buildGptOssPrompt, callGemini, callGroq } from '../lib/llm.js';

export default async function handler(req, res) {
  // Allow preflight OPTIONS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Yaqeen-App-Key');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed. Only POST is accepted.' });
  }

  // 1. Shared App Secret Check (Abuse friction)
  const appSecretEnv = process.env.YAQEEN_APP_SECRET;
  const providedSecret = req.headers['x-yaqeen-app-key'] || req.headers['x-yaqeen-app-key'.toLowerCase()];

  if (appSecretEnv && providedSecret !== appSecretEnv) {
    return res.status(401).json({ error: 'Unauthorized: missing or invalid app key' });
  }

  // 2. Client IP & Per-IP Rate Limiting
  const forwarded = req.headers['x-forwarded-for'];
  const clientIp = typeof forwarded === 'string'
    ? forwarded.split(',')[0].trim()
    : req.socket?.remoteAddress || '127.0.0.1';

  const ipLimit = parseInt(process.env.PER_IP_LIMIT_PER_MINUTE || '15', 10);
  const ipCheck = await checkIpRateLimit(clientIp, ipLimit);
  if (!ipCheck.allowed) {
    return res.status(429).json({
      error: 'Rate limit exceeded for this IP. Please wait a minute before requesting another source check.'
    });
  }

  // 3. Persistent Budget Kill-Switch
  const dailyCap = parseInt(process.env.DAILY_REQUEST_CAP || '200', 10);
  const killSwitch = await checkDailyKillSwitch(dailyCap);
  if (!killSwitch.allowed) {
    return res.status(503).json({
      error: 'Daily service capacity reached. To prevent billing overrun, the service is paused until tomorrow.'
    });
  }

  // 4. Request Payload Parsing
  const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : (req.body || {});
  const { url, provider, model, headline_hint } = body;

  if (!url || typeof url !== 'string') {
    return res.status(400).json({ error: "Missing or invalid 'url' parameter" });
  }

  const selectedProvider = (provider || 'groq').toLowerCase();
  const selectedModel = (model && model.includes('qwen')) ? 'qwen/qwen3.8-27b' : 'openai/gpt-oss-120b';

  // 5. SSRF-Safe URL Fetch
  let fetchedHtml = '';
  let fetchFailed = false;
  let fetchErrorMessage = '';

  try {
    const fetchResult = await ssrfSafeFetch(url);
    fetchedHtml = fetchResult.html;
  } catch (err) {
    const isSsrf = err.message.includes('SSRF') || err.message.includes('restricted');
    if (isSsrf) {
      return res.status(400).json({
        error: 'Invalid URL: restricted destination or IP address.'
      });
    }
    // Remote site blocked fetch (e.g. Cloudflare, timeout, 403)
    fetchFailed = true;
    fetchErrorMessage = err.message;
  }

  // 6. Article Extraction & Paywall Detection
  let extracted = {
    title: headline_hint || '',
    author: '',
    publisher: '',
    text: '',
    isTooShort: false
  };

  if (!fetchFailed && fetchedHtml) {
    extracted = extractArticleText(fetchedHtml);
  }

  // If fetch failed or body is empty/paywalled, provide fallback context for web-grounded models
  let articleContentForPrompt = extracted.text;
  if (fetchFailed || extracted.isTooShort) {
    articleContentForPrompt = `[Note: Direct web scraping encountered an issue (${fetchErrorMessage || 'paywalled or JavaScript required'}). Use your knowledge and live web search capabilities to assess the publication, headline, and reported claims.]`;
  }

  // 7. Prompt Construction
  // Full article prompt for deep analytical models (Qwen 3.8 and Gemini)
  const prompt = buildDelimitedPrompt(
    url,
    extracted.title || headline_hint || 'News Article',
    extracted.author,
    extracted.publisher,
    articleContentForPrompt
  );

  // Rate-limit optimized prompt for GPT-OSS (openai/gpt-oss-120b) to prevent HTTP 413 request_too_large and preserve 8k TPM headroom
  const gptOssPrompt = buildGptOssPrompt(
    url,
    extracted.title || headline_hint || 'News Article',
    extracted.author,
    extracted.publisher,
    articleContentForPrompt
  );

  // 8. LLM Provider Execution
  try {
    let result;
    if (selectedProvider === 'gemini') {
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(503).json({ error: 'Gemini source-check is not configured on the proxy.' });
      }
      result = await callGemini(apiKey, prompt, selectedModel);
    } else {
      const apiKey = process.env.GROQ_API_KEY;
      if (!apiKey) {
        return res.status(503).json({ error: 'Groq source-check is not configured on the proxy.' });
      }
      result = await callGroq(apiKey, prompt, selectedModel, gptOssPrompt);
    }

    // Attach extracted metadata if LLM didn't fill it
    if (!result.headline && extracted.title) result.headline = extracted.title;
    if (!result.author && extracted.author) result.author = extracted.author;
    if (!result.publisher && extracted.publisher) result.publisher = extracted.publisher;
    result.url = url;

    return res.status(200).json(result);
  } catch (err) {
    console.error(`Error calling ${selectedProvider} (${selectedModel}):`, err);
    if (err.isUserSwitchPrompt) {
      return res.status(err.status || 400).json({
        error: err.message
      });
    }
    if (err.status === 429 || err.message?.includes('429') || err.message?.includes('quota') || err.message?.includes('rate limit')) {
      return res.status(429).json({
        error: err.message || `The ${selectedProvider} service is currently rate-limited. Please switch to another model in Settings or try again in a few moments.`
      });
    }

    return res.status(500).json({
      error: `Error obtaining source verification verdict: ${err.message}`
    });
  }
}
