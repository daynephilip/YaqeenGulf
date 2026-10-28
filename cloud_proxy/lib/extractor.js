import * as cheerio from 'cheerio';

/**
 * Extracts clean article title and readable text from raw HTML.
 * @param {string} html
 * @param {number} maxChars (default 3000)
 * @returns {{ title: string, text: string, charCount: number, isTooShort: boolean }}
 */
export function extractArticleText(html, maxChars = 3000) {
  if (!html || typeof html !== 'string') {
    return { title: '', author: '', publisher: '', text: '', charCount: 0, isTooShort: true };
  }

  const $ = cheerio.load(html);

  // Extract author before stripping metadata
  let author = $('meta[name="author"]').attr('content') ||
               $('meta[property="article:author"]').attr('content') ||
               $('meta[name="twitter:creator"]').attr('content') ||
               $('a[rel="author"], .author, .byline, [class*="byline"], [class*="author"]').first().text().replace(/\s+/g, ' ').trim() ||
               '';
  if (author.length > 80) author = author.slice(0, 80);

  // Extract publisher / site name
  let publisher = $('meta[property="og:site_name"]').attr('content') ||
                  $('meta[name="publisher"]').attr('content') ||
                  $('meta[name="application-name"]').attr('content') ||
                  '';
  if (publisher.length > 80) publisher = publisher.slice(0, 80);

  // Extract title
  let title = $('meta[property="og:title"]').attr('content') ||
              $('meta[name="twitter:title"]').attr('content') ||
              $('title').text() ||
              $('h1').first().text() ||
              '';
  title = title.replace(/\s+/g, ' ').trim();

  // Remove elements that definitely do not contain article body content
  $(
    'script, style, noscript, svg, iframe, header, footer, nav, aside, ' +
    'form, button, input, select, textarea, ' +
    '.ad, .ads, .advertisement, .ad-box, .banner, .cookie-notice, ' +
    '.social-share, .comments, .related-posts, .sidebar, .popup'
  ).remove();

  // Target common article containers if available
  let articleEl = $('article, main, [role="main"], .article-body, .article-content, .story-body, .post-content, .entry-content');
  if (!articleEl.length) {
    articleEl = $('body');
  }

  // Gather text from paragraphs and headings
  const textBlocks = [];
  articleEl.find('p, h2, h3, h4, blockquote, li').each((_, el) => {
    const blockText = $(el).text().replace(/\s+/g, ' ').trim();
    // Skip very short fragments, disclaimers, or noise
    if (blockText.length > 20) {
      textBlocks.push(blockText);
    }
  });

  let fullText = textBlocks.join('\n\n').trim();

  // If paragraph extraction yielded very little, fallback to raw clean body text
  if (fullText.length < 50) {
    fullText = articleEl.text().replace(/\s+/g, ' ').trim();
  }

  const charCount = fullText.length;
  const isTooShort = charCount < 80;

  // Truncate to maxChars to cap LLM token cost and prompt injection attack surface
  let truncatedText = fullText;
  if (truncatedText.length > maxChars) {
    truncatedText = truncatedText.slice(0, maxChars) + '... [Content truncated for length]';
  }

  return {
    title,
    author,
    publisher,
    text: truncatedText,
    charCount,
    isTooShort
  };
}
