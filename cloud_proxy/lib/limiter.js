import { Redis } from '@upstash/redis';

// In-memory fallback store for local development/testing when Upstash credentials are not configured
const memoryStore = new Map();

/**
 * Returns a Redis client instance if credentials are provided in environment variables,
 * or null to indicate fallback to mock/memory store.
 */
function getRedisClient() {
  const url = process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.UPSTASH_REDIS_REST_TOKEN;
  if (url && token && !url.includes('example.com') && !token.includes('placeholder')) {
    try {
      return new Redis({ url, token });
    } catch {
      return null;
    }
  }
  return null;
}

/**
 * Checks and increments persistent daily request counter (budget kill-switch).
 * @param {number} dailyCap
 * @param {object} [customRedis] (optional injection for tests)
 * @returns {Promise<{ allowed: boolean, count: number, cap: number, dateKey: string }>}
 */
export async function checkDailyKillSwitch(dailyCap = 100, customRedis = null) {
  const now = new Date();
  const dateKey = `yaqeen:daily_reqs:${now.toISOString().slice(0, 10)}`;
  const redis = customRedis || getRedisClient();

  if (redis) {
    try {
      const current = await redis.incr(dateKey);
      if (current === 1) {
        // Set TTL of 48 hours to automatically expire old day keys
        await redis.expire(dateKey, 172800);
      }
      return {
        allowed: current <= dailyCap,
        count: current,
        cap: dailyCap,
        dateKey
      };
    } catch (err) {
      console.error('Redis error during kill-switch check:', err.message);
      // Fail open or use local memory fallback if Redis temporarily errors
    }
  }

  // In-memory fallback
  const current = (memoryStore.get(dateKey) || 0) + 1;
  memoryStore.set(dateKey, current);
  return {
    allowed: current <= dailyCap,
    count: current,
    cap: dailyCap,
    dateKey
  };
}

/**
 * Checks and increments persistent per-IP rate limit.
 * @param {string} clientIp
 * @param {number} limitPerMinute
 * @param {object} [customRedis]
 * @returns {Promise<{ allowed: boolean, count: number, limit: number }>}
 */
export async function checkIpRateLimit(clientIp, limitPerMinute = 10, customRedis = null) {
  const sanitizedIp = (clientIp || 'unknown').replace(/[^a-zA-Z0-9_.-]/g, '_');
  const minuteWindow = Math.floor(Date.now() / 60000);
  const key = `yaqeen:rate_ip:${sanitizedIp}:${minuteWindow}`;
  const redis = customRedis || getRedisClient();

  if (redis) {
    try {
      const current = await redis.incr(key);
      if (current === 1) {
        await redis.expire(key, 65);
      }
      return {
        allowed: current <= limitPerMinute,
        count: current,
        limit: limitPerMinute
      };
    } catch (err) {
      console.error('Redis error during IP rate-limit check:', err.message);
    }
  }

  // In-memory fallback
  const current = (memoryStore.get(key) || 0) + 1;
  memoryStore.set(key, current);
  return {
    allowed: current <= limitPerMinute,
    count: current,
    limit: limitPerMinute
  };
}

/**
 * Reset memory store (used in unit tests)
 */
export function resetMemoryStore() {
  memoryStore.clear();
}
