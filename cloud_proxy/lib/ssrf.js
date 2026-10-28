import dns from 'node:dns/promises';
import ipaddr from 'ipaddr.js';

/**
 * Checks whether an IP string is restricted (private, loopback, link-local, cloud metadata, multicast, etc.)
 * @param {string} ipStr
 * @returns {boolean} true if IP is blocked / unsafe
 */
export function isRestrictedIp(ipStr) {
  try {
    let addr = ipaddr.parse(ipStr);
    // If IPv4-mapped IPv6 address, unmap to IPv4
    if (addr.kind() === 'ipv6' && addr.isIPv4MappedAddress()) {
      addr = addr.toIPv4Address();
    }

    const range = addr.range();
    const blockedRanges = [
      'loopback',
      'private',
      'linkLocal',
      'uniqueLocal',
      'unspecified',
      'multicast',
      'broadcast',
      'carrierGradeNat',
      'reserved'
    ];

    if (blockedRanges.includes(range)) {
      return true;
    }

    // Explicit cloud metadata checks (AWS/GCP/Azure 169.254.169.254)
    if (addr.kind() === 'ipv4') {
      const octets = addr.octets;
      if (octets[0] === 169 && octets[1] === 254) {
        return true;
      }
      if (octets[0] === 127 || octets[0] === 10 || octets[0] === 0) {
        return true;
      }
      if (octets[0] === 192 && octets[1] === 168) {
        return true;
      }
      if (octets[0] === 172 && (octets[1] >= 16 && octets[1] <= 31)) {
        return true;
      }
    }

    return false;
  } catch {
    // If it cannot be parsed as a valid IP, block it by default
    return true;
  }
}

/**
 * Validates a target URL and its resolved IPs against SSRF restrictions.
 * @param {string} urlStr
 * @returns {Promise<{ valid: boolean, error?: string, parsedUrl?: URL, resolvedIps?: string[] }>}
 */
export async function validateSsrfUrl(urlStr) {
  let parsedUrl;
  try {
    parsedUrl = new URL(urlStr);
  } catch {
    return { valid: false, error: 'Malformed or unparseable URL' };
  }

  // Scheme must be strictly http or https
  if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
    return { valid: false, error: `Invalid scheme '${parsedUrl.protocol}'. Only http: and https: are allowed.` };
  }

  const hostname = parsedUrl.hostname.trim().toLowerCase();
  if (!hostname || hostname === 'localhost') {
    return { valid: false, error: 'Localhost or empty hostname is rejected.' };
  }

  // If hostname is directly an IP literal (e.g. 127.0.0.1, 169.254.169.254, [::1])
  let isIpLiteral = false;
  let normalizedHost = hostname;
  if (normalizedHost.startsWith('[') && normalizedHost.endsWith(']')) {
    normalizedHost = normalizedHost.slice(1, -1);
  }

  try {
    if (ipaddr.isValid(normalizedHost)) {
      isIpLiteral = true;
      if (isRestrictedIp(normalizedHost)) {
        return { valid: false, error: `Target IP ${normalizedHost} belongs to a restricted/private IP range.` };
      }
    }
  } catch {
    // Not an IP literal
  }

  // Resolve hostname via DNS
  if (!isIpLiteral) {
    try {
      const addresses = await dns.lookup(hostname, { all: true });
      if (!addresses || addresses.length === 0) {
        return { valid: false, error: `DNS lookup failed for hostname '${hostname}'` };
      }

      const resolvedIps = addresses.map(a => a.address);
      for (const ip of resolvedIps) {
        if (isRestrictedIp(ip)) {
          return {
            valid: false,
            error: `Hostname '${hostname}' resolved to restricted IP address '${ip}'`,
            resolvedIps
          };
        }
      }

      return { valid: true, parsedUrl, resolvedIps };
    } catch (err) {
      return { valid: false, error: `DNS resolution failed for '${hostname}': ${err.message}` };
    }
  }

  return { valid: true, parsedUrl, resolvedIps: [normalizedHost] };
}

/**
 * Performs an SSRF-safe HTTP fetch with redirect re-validation and timeout.
 * @param {string} initialUrl
 * @param {number} maxRedirects
 * @param {number} timeoutMs
 * @param {Function} fetchImpl (optional dependency injection for tests)
 * @returns {Promise<{ html: string, finalUrl: string }>}
 */
export async function ssrfSafeFetch(initialUrl, maxRedirects = 3, timeoutMs = 8000, fetchImpl = fetch) {
  let currentUrl = initialUrl;
  let redirectCount = 0;

  while (redirectCount <= maxRedirects) {
    const validation = await validateSsrfUrl(currentUrl);
    if (!validation.valid) {
      throw new Error(`SSRF validation rejected URL: ${validation.error}`);
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    let res;
    try {
      res = await fetchImpl(currentUrl, {
        method: 'GET',
        redirect: 'manual', // Manual redirect control to re-validate target IP on each hop
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
          'Accept-Language': 'en-US,en;q=0.9,ar;q=0.8',
          'Sec-Ch-Ua': '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"',
          'Sec-Ch-Ua-Mobile': '?0',
          'Sec-Ch-Ua-Platform': '"Windows"',
          'Sec-Fetch-Dest': 'document',
          'Sec-Fetch-Mode': 'navigate',
          'Sec-Fetch-Site': 'none',
          'Sec-Fetch-User': '?1',
          'Upgrade-Insecure-Requests': '1'
        },
        signal: controller.signal
      });
    } catch (err) {
      clearTimeout(timeout);
      if (err.name === 'AbortError') {
        throw new Error(`Request timed out after ${timeoutMs}ms`);
      }
      throw err;
    } finally {
      clearTimeout(timeout);
    }

    // Check for redirect status codes
    if ([301, 302, 303, 307, 308].includes(res.status)) {
      const location = res.headers.get('location');
      if (!location) {
        throw new Error(`Redirect response (${res.status}) missing 'location' header.`);
      }
      // Resolve relative redirect URLs against the current URL
      currentUrl = new URL(location, currentUrl).toString();
      redirectCount++;
      if (redirectCount > maxRedirects) {
        throw new Error(`Exceeded maximum redirect limit of ${maxRedirects}`);
      }
      continue;
    }

    if (!res.ok) {
      throw new Error(`HTTP error: remote server responded with status ${res.status}`);
    }

    const contentType = res.headers.get('content-type') || '';
    if (!contentType.includes('text/html') && !contentType.includes('text/plain') && !contentType.includes('application/xhtml+xml')) {
      // Still attempt reading text, but flag if binary
      if (contentType.includes('image/') || contentType.includes('video/') || contentType.includes('audio/') || contentType.includes('application/pdf')) {
        throw new Error(`Unsupported content type '${contentType}'. Only web articles are supported.`);
      }
    }

    const html = await res.text();
    return { html, finalUrl: currentUrl };
  }

  throw new Error(`Exceeded maximum redirect limit of ${maxRedirects}`);
}
