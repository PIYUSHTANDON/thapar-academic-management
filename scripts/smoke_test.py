"""Exercise a real Java server using ephemeral, environment-configured synthetic users.
Run after Maven package: python scripts/smoke_test.py
No credentials or database files are retained.
"""
from pathlib import Path
from urllib.request import Request, build_opener, HTTPCookieProcessor
from urllib.error import HTTPError, URLError
from http.cookiejar import CookieJar
from datetime import datetime, timedelta
import json, os, secrets, subprocess, time, re
settings = subprocess.run(["java", "-XshowSettings:properties", "-version"], capture_output=True, text=True, check=True)
java_home = re.search(r"java.home = (.+)", settings.stderr).group(1).strip()
JAVA = str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java"))
ROOT = Path(__file__).resolve().parents[1]
env = os.environ.copy()
faculty_id, student_id = "qa-faculty-" + secrets.token_hex(4), "qa-student-" + secrets.token_hex(4)
faculty_password, student_password = secrets.token_urlsafe(24), secrets.token_urlsafe(24)
env.update(SPRING_PROFILES_ACTIVE="local", SEED_ENABLED="true", SEED_FACULTY_ID=faculty_id, SEED_FACULTY_PASSWORD=faculty_password, SEED_STUDENT_ID=student_id, SEED_STUDENT_PASSWORD=student_password, SERVER_PORT="8081", SPRING_DATASOURCE_URL="jdbc:h2:mem:http-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1", LOGGING_LEVEL_ROOT="WARN", DEBUG="false")
opener = build_opener(HTTPCookieProcessor(CookieJar()))
csrf = None
checks = 0

def request(path, method="GET", body=None, expected=200, raw=None, content_type=None):
    global checks
    headers = {}
    if method != "GET" and csrf:
        headers[csrf["headerName"]] = csrf["token"]
    data = raw
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    req = Request("http://127.0.0.1:8081/api/v1" + path, data=data, method=method, headers=headers)
    try:
        with opener.open(req, timeout=10) as res:
            status, data = res.status, res.read()
    except HTTPError as error:
        status, data = error.code, error.read()
    assert status == expected, f"{method} {path}: expected {expected}, received {status}: {data[:250]!r}"
    checks += 1
    return json.loads(data) if data else None

(ROOT / ".tools").mkdir(exist_ok=True)
with open(ROOT / ".tools/http-smoke.log", "w", encoding="utf-8") as log:
    process = subprocess.Popen([JAVA, "-jar", str(ROOT / "backend/target/ams-1.0.0.jar")], cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        for attempt in range(90):
            if process.poll() is not None:
                raise RuntimeError("Java server exited; see .tools/http-smoke.log")
            try:
                with opener.open("http://127.0.0.1:8081/api/v1/health", timeout=2) as ready:
                    assert ready.status == 200
                csrf = request("/auth/csrf")
                break
            except (URLError, ConnectionError):
                time.sleep(1)
        else:
            raise RuntimeError("Java server did not become ready")
        request("/faculty/dashboard", expected=401)
        user = request("/auth/login", "POST", {"identifier":faculty_id,"password":faculty_password,"role":"FACULTY","rememberMe":True})
        assert user["role"] == "FACULTY"
        csrf = request("/auth/csrf")
        request("/auth/me")
        for path in ["dashboard","courses","schedule","students","notifications","messages","attendance","assessments","calendar","submissions"]:
            request("/faculty/" + path)
        courses = request("/faculty/courses")
        course = courses[0]
        roster = request("/faculty/students")
        request("/faculty/messages", "POST", {"receiverId":roster[0]["userId"],"body":"Synthetic real HTTP smoke-test message"})
        assert any(m["body"] == "Synthetic real HTTP smoke-test message" for m in request("/faculty/messages"))
        request(f"/faculty/courses/{course['id']}", "PUT", {"title":course["title"],"description":"Updated by synthetic smoke test"})
        request(f"/faculty/courses/{course['id']}/announcements", "POST", {"title":"Synthetic course announcement"})
        boundary = "----AmsSmokeBoundary"
        payload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="demo.txt"\r\nContent-Type: text/plain\r\n\r\nSynthetic learning resource\r\n--{boundary}--\r\n').encode()
        request(f"/faculty/courses/{course['id']}/resources", "POST", raw=payload, content_type="multipart/form-data; boundary="+boundary)
        resources = request(f"/faculty/courses/{course['id']}/resources")
        assert any(r["title"] == "demo.txt" for r in resources)
        created = request("/faculty/assessments", "POST", {"courseId":course["id"],"title":"HTTP smoke assessment","kind":"Quiz","deadline":(datetime.now()+timedelta(days=2)).isoformat(timespec="seconds")})
        request(f"/faculty/assessments/{created['id']}/publish", "POST")
        submission = request("/faculty/submissions")[0]
        request(f"/faculty/submissions/{submission['id']}/grade", "POST", {"marks":87})
        request("/faculty/calendar", "POST", {"title":"Synthetic academic event","eventDate":(datetime.now()+timedelta(days=1)).date().isoformat()})
        notification = request("/faculty/notifications")[0]
        request(f"/faculty/notifications/{notification['id']}/read", "POST")
        request("/faculty/settings/profile", "PUT", {"name":"Synthetic HTTP Faculty","notificationsEnabled":False})
        assert request("/auth/me")["name"] == "Synthetic HTTP Faculty"
        request("/auth/logout", "POST", expected=204)
        request("/faculty/dashboard", expected=401)
        csrf = request("/auth/csrf")
        request("/auth/login", "POST", {"identifier":student_id,"password":student_password,"role":"STUDENT"})
        csrf = request("/auth/csrf")
        request("/faculty/dashboard", expected=403)
        request("/auth/logout", "POST", expected=204)
        print(f"PASS: {checks} real HTTP checks; frontend/backend operations and role boundaries verified.")
    finally:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
