import 'dotenv/config';
import http from 'node:http';
import handler from './check.js';

const PORT = process.env.PORT || 3000;

const server = http.createServer(async (req, res) => {
  res.status = (code) => {
    res.statusCode = code;
    return res;
  };
  res.json = (data) => {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(data));
    return res;
  };

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  if (url.pathname === '/check' || url.pathname === '/api/check') {
    let bodyStr = '';
    req.on('data', (chunk) => { bodyStr += chunk; });
    req.on('end', async () => {
      try {
        req.body = bodyStr ? JSON.parse(bodyStr) : {};
      } catch {
        req.body = {};
      }
      try {
        await handler(req, res);
      } catch (err) {
        console.error('Handler error:', err);
        if (!res.writableEnded) {
          res.status(500).json({ error: err.message });
        }
      }
    });
  } else {
    res.status(404).json({ error: 'Endpoint not found' });
  }
});

server.listen(PORT, () => {
  console.log(`Yaqeen Cloud Proxy running at http://localhost:${PORT}`);
});
