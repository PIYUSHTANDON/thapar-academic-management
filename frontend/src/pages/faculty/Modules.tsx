import { useState, useEffect, type FormEvent } from "react";
import { useLocation, Link, useNavigate } from "react-router-dom";
import {
  BookOpen,
  Search,
  Send,
  Plus,
  Upload,
  Download,
  Check,
  ArrowLeft,
  QrCode,
} from "lucide-react";
import { api, errorText, refreshCsrf } from "../../services/api";
import { Panel, Empty, CalendarWidget, type Event } from "../../components/ui";
import { useAuth } from "../../hooks/useAuth";
import FacultyScheduleEditor from "../../components/FacultyScheduleEditor";
type Row = Record<string, any>;
export default function Modules() {
  const module = useLocation().pathname.split("/")[2];
  const { user, setUser } = useAuth();
  const go = useNavigate();
  const [courses, setCourses] = useState<Row[]>([]),
    [students, setStudents] = useState<Row[]>([]),
    [rows, setRows] = useState<Row[]>([]),
    [sessions, setSessions] = useState<Row[]>([]),
    [submissions, setSubmissions] = useState<Row[]>([]),
    [resources, setResources] = useState<Row[]>([]),
    [course, setCourse] = useState(""),
    [recipient, setRecipient] = useState(""),
    [query, setQuery] = useState(""),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [attendanceToken, setAttendanceToken] = useState<Row | null>(null),
    [qrImage, setQrImage] = useState(""),
    [seconds, setSeconds] = useState(0);
  const title: Record<string, string> = {
    messages: "Messages",
    attendance: "Attendance",
    courses: "Courses & LMS",
    assessments: "Assessments",
    calendar: "Academic Calendar",
    students: "Student Roster",
    settings: "Settings",
    reports: "Attendance Reports",
  };
  async function load() {
    setLoading(true);
    setError("");
    try {
      const [c, st] = await Promise.all([
        api.get("/faculty/courses"),
        api.get("/faculty/students"),
      ]);
      setCourses(c.data);
      setStudents(st.data);
      if (!course && c.data.length) setCourse(String(c.data[0].id));
      if (!recipient && st.data.length) setRecipient(String(st.data[0].userId));
      const endpoint = module === "reports" ? "attendance" : module;
      if (
        [
          "messages",
          "attendance",
          "assessments",
          "calendar",
          "reports",
        ].includes(module)
      ) {
        setRows((await api.get("/faculty/" + endpoint)).data);
      }
      if (["attendance", "reports"].includes(module))
        setSessions((await api.get("/faculty/schedule")).data);
      if (module === "assessments")
        setSubmissions((await api.get("/faculty/submissions")).data);
      return true;
    } catch (e) {
      setError(errorText(e));
      return false;
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    setRows([]);
    setToast("");
    setAttendanceToken(null);
    load();
  }, [module]);
  useEffect(() => {
    if (module === "courses" && course)
      api
        .get("/faculty/courses/" + course + "/resources")
        .then((r) => setResources(r.data))
        .catch((e) => setError(errorText(e)));
  }, [course, module, toast]);
  useEffect(() => {
    if (!attendanceToken) {
      setQrImage("");
      return;
    }
    let alive = true;
    import("qrcode")
      .then((q) =>
        q.toDataURL(
          JSON.stringify({
            type: "thapar-attendance",
            token: attendanceToken.token,
            sessionId: attendanceToken.sessionId,
          }),
          { width: 240, margin: 2 },
        ),
      )
      .then((url) => {
        if (alive) setQrImage(url);
      });
    const tick = () =>
      setSeconds(
        Math.max(
          0,
          Math.ceil(
            (new Date(attendanceToken.expiresAt + "+05:30").getTime() -
              Date.now()) /
              1000,
          ),
        ),
      );
    tick();
    const t = setInterval(tick, 1000);
    return () => {
      alive = false;
      clearInterval(t);
    };
  }, [attendanceToken]);
  async function action(
    fn: () => Promise<unknown>,
    message: string,
    refresh = true,
  ) {
    setBusy(true);
    setError("");
    setToast("");
    try {
      await fn();
      if (!refresh || (await load())) setToast(message);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  function fields(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    return Object.fromEntries(new FormData(e.currentTarget));
  }
  const shownStudents = students.filter(
    (s) =>
      (!course || String(s.courseId) === course) &&
      (s.name + " " + s.enrollmentNumber)
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  return (
    <main className="module-page">
      <div className="module-heading">
        <div>
          <p className="eyebrow">FACULTY WORKSPACE</p>
          <h1>{title[module] || "Faculty module"}</h1>
          <p className="muted">
            {module === "messages"
              ? "Stay connected with students in your assigned courses."
              : module === "attendance"
                ? "Manage secure attendance for your teaching sessions."
                : module === "courses"
                  ? "Your teaching, resources, and announcements in one place."
                  : module === "assessments"
                    ? "Create meaningful assessments and support student progress."
                    : module === "students"
                      ? "Academic information for students enrolled in your courses."
                      : module === "settings"
                        ? "Make this workspace yours."
                        : "Plan, review, and keep your academic work on track."}
          </p>
        </div>
        <Link className="back-link" to="/faculty/dashboard">
          <ArrowLeft size={16} /> Dashboard
        </Link>
      </div>
      {error && (
        <div className="error" role="alert">
          {error}
          <button onClick={load}>Retry</button>
        </div>
      )}
      {toast && (
        <div className="toast" role="status">
          <Check size={18} />
          {toast}
        </div>
      )}
      {loading && (
        <p className="loading" role="status">
          Loading your workspace…
        </p>
      )}
      {module === "students" && (
        <Panel title="Enrolled students">
          <div className="filters">
            <div className="input-icon">
              <Search size={18} />
              <input
                aria-label="Search students"
                placeholder="Search name or enrollment number"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <select
              aria-label="Filter by course"
              value={course}
              onChange={(e) => setCourse(e.target.value)}
            >
              <option value="">All assigned courses</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.title} · {c.section}
                </option>
              ))}
            </select>
          </div>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Enrollment</th>
                  <th>Program</th>
                  <th>Course</th>
                </tr>
              </thead>
              <tbody>
                {shownStudents.map((s) => (
                  <tr key={s.id + "-" + s.courseId}>
                    <td>
                      <strong>{s.name}</strong>
                    </td>
                    <td>{s.enrollmentNumber}</td>
                    <td>{s.program}</td>
                    <td>{s.course}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!shownStudents.length && (
              <Empty>No students match this selection.</Empty>
            )}
          </div>
        </Panel>
      )}
      {module === "messages" && (
        <div className="message-layout">
          <Panel title="Conversations">
            <div className="conversation-list">
              {students
                .filter(
                  (s, i, a) => a.findIndex((x) => x.userId === s.userId) === i,
                )
                .map((s) => (
                  <button
                    className={recipient === String(s.userId) ? "active" : ""}
                    onClick={() => setRecipient(String(s.userId))}
                    key={s.userId}
                  >
                    <span className="avatar">{s.name[0]}</span>
                    <span>
                      <strong>{s.name}</strong>
                      <small>{s.enrollmentNumber}</small>
                    </span>
                  </button>
                ))}
              {!students.length && (
                <Empty>No enrolled students to message.</Empty>
              )}
            </div>
          </Panel>
          <Panel
            title={
              students.find((s) => String(s.userId) === recipient)?.name ||
              "Select a conversation"
            }
          >
            <div className="message-history">
              {rows
                .filter(
                  (m) =>
                    String(m.senderId) === recipient ||
                    String(m.receiverId) === recipient,
                )
                .map((m) => (
                  <div
                    className={
                      "message " + (m.senderId === user?.id ? "outgoing" : "")
                    }
                    key={m.id}
                  >
                    <small>{m.sender}</small>
                    <p>{m.body}</p>
                    <time>{new Date(m.sentAt).toLocaleString("en-IN")}</time>
                  </div>
                ))}
              {!rows.some(
                (m) =>
                  String(m.senderId) === recipient ||
                  String(m.receiverId) === recipient,
              ) && <Empty>Start a conversation with your student.</Empty>}
            </div>
            <form
              className="compose"
              onSubmit={(e) => {
                const f = fields(e);
                const form = e.currentTarget;
                action(async () => {
                  await api.post("/faculty/messages", {
                    receiverId: Number(recipient),
                    body: f.body,
                  });
                  form.reset();
                }, "Message sent");
              }}
            >
              <input
                aria-label="Message"
                name="body"
                placeholder="Write a message…"
                required
                maxLength={4000}
              />
              <button
                disabled={!recipient || busy}
                className="primary"
                aria-label="Send message"
              >
                <Send size={19} />
              </button>
            </form>
          </Panel>
        </div>
      )}
      {module === "courses" && (
        <div className="course-layout">
          <div className="course-list">
            {courses.map((c) => (
              <button
                className={
                  "course-card " + (course === String(c.id) ? "active" : "")
                }
                key={c.id}
                onClick={() => setCourse(String(c.id))}
              >
                <BookOpen />
                <small>
                  {c.code} · {c.section}
                </small>
                <h2>{c.title}</h2>
                <p>{c.students} enrolled students</p>
              </button>
            ))}
            {!courses.length && <Empty>No courses assigned.</Empty>}
          </div>
          {course && (
            <div className="module-stack">
              <Panel title="Course information">
                <form
                  className="stack-form"
                  key={course}
                  onSubmit={(e) => {
                    const f = fields(e);
                    action(
                      () => api.put("/faculty/courses/" + course, f),
                      "Course updated",
                    );
                  }}
                >
                  <label>
                    Course title
                    <input
                      required
                      name="title"
                      maxLength={255}
                      defaultValue={
                        courses.find((c) => String(c.id) === course)?.title
                      }
                    />
                  </label>
                  <label>
                    Description
                    <textarea
                      name="description"
                      maxLength={2000}
                      defaultValue={
                        courses.find((c) => String(c.id) === course)
                          ?.description
                      }
                    />
                  </label>
                  <button className="primary" disabled={busy}>
                    Save course
                  </button>
                </form>
              </Panel>
              <Panel title="Learning resources">
                <form
                  className="upload-form"
                  onSubmit={(e) => {
                    e.preventDefault();
                    const data = new FormData(e.currentTarget);
                    action(
                      () =>
                        api.post(
                          "/faculty/courses/" + course + "/resources",
                          data,
                        ),
                      "Resource uploaded",
                    );
                  }}
                >
                  <input
                    name="file"
                    aria-label="Learning resource file"
                    type="file"
                    required
                  />
                  <button className="secondary" disabled={busy}>
                    <Upload size={17} /> Upload
                  </button>
                </form>
                {resources.map((r) => (
                  <div className="resource-row" key={r.id}>
                    <span>
                      <small>{r.kind}</small>
                      <strong>{r.title}</strong>
                    </span>
                    {r.kind === "File" && (
                      <a
                        aria-label={"Download " + r.title}
                        href={"/api/v1/faculty/resources/" + r.id}
                      >
                        <Download size={19} />
                      </a>
                    )}
                  </div>
                ))}
                {!resources.length && <Empty>No resources shared yet.</Empty>}
              </Panel>
              <Panel title="Create announcement">
                <form
                  className="stack-form"
                  onSubmit={(e) => {
                    const f = fields(e);
                    action(
                      () =>
                        api.post(
                          "/faculty/courses/" + course + "/announcements",
                          f,
                        ),
                      "Announcement shared with enrolled students",
                    );
                  }}
                >
                  <label>
                    Announcement
                    <input
                      name="title"
                      required
                      maxLength={255}
                      placeholder="What should your students know?"
                    />
                  </label>
                  <label>
                    Announcement description
                    <textarea name="description" maxLength={4000} />
                  </label>
                  <button className="primary" disabled={busy}>
                    Publish announcement
                  </button>
                </form>
              </Panel>
            </div>
          )}
        </div>
      )}
      {module === "assessments" && (
        <div className="two-column">
          <div className="module-stack">
            <Panel title="Assessments">
              {rows.map((a) => (
                <div className="assessment-item" key={a.id}>
                  <div>
                    <small>
                      {a.course} · {a.kind}
                    </small>
                    <h3>{a.title}</h3>
                    <p>Due {new Date(a.deadline).toLocaleString("en-IN")}</p>
                  </div>
                  <span
                    className={
                      "badge " + (a.published ? "completed" : "upcoming")
                    }
                  >
                    {a.published ? "Published" : "Draft"}
                  </span>
                  {true && (
                    <button
                      className="secondary"
                      disabled={busy}
                      onClick={() =>
                        action(
                          () =>
                            api.post(
                              "/faculty/assessments/" + a.id + "/publish",
                            ),
                          "Results published",
                        )
                      }
                    >
                      {a.published ? "Publish updates" : "Publish results"}
                    </button>
                  )}
                </div>
              ))}
              {!rows.length && <Empty>No assessments yet.</Empty>}
            </Panel>
            <Panel title="Student submissions">
              {submissions.map((s) => (
                <article className="submission" key={s.id}>
                  <h3>{s.assessment}</h3>
                  <small>{s.student}</small>
                  <p>{s.content}</p>
                  {s.fileName && (
                    <a
                      className="text-link"
                      href={
                        api.defaults.baseURL +
                        "/faculty/submissions/" +
                        s.id +
                        "/file"
                      }
                    >
                      Download {s.fileName}
                    </a>
                  )}
                  <form
                    className="inline-form"
                    onSubmit={(e) => {
                      const f = fields(e);
                      action(
                        () =>
                          api.post("/faculty/submissions/" + s.id + "/grade", {
                            marks: Number(f.marks),
                            feedback: f.feedback,
                          }),
                        "Grade saved",
                      );
                    }}
                  >
                    <label>
                      Marks / 100
                      <input
                        type="number"
                        name="marks"
                        min="0"
                        max="100"
                        required
                        defaultValue={s.marks === "Ungraded" ? "" : s.marks}
                      />
                    </label>
                    <label>
                      Faculty feedback
                      <textarea
                        name="feedback"
                        maxLength={2000}
                        defaultValue={s.feedback}
                      />
                    </label>
                    <button className="secondary" disabled={busy}>
                      Save grade
                    </button>
                  </form>
                </article>
              ))}
              {!submissions.length && <Empty>No submissions received.</Empty>}
            </Panel>
          </div>
          <Panel title="Create assessment" icon={<Plus size={20} />}>
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(
                  () =>
                    api.post("/faculty/assessments", {
                      ...f,
                      courseId: Number(f.courseId),
                    }),
                  "Assessment created",
                );
              }}
            >
              <CourseSelect courses={courses} />
              <label>
                Title
                <input
                  name="title"
                  required
                  maxLength={255}
                  placeholder="e.g. Data structures problem set"
                />
              </label>
              <label>
                Type
                <select name="kind">
                  <option>Assignment</option>
                  <option>Quiz</option>
                </select>
              </label>
              <label>
                Deadline
                <input type="datetime-local" name="deadline" required />
              </label>
              <button className="primary" disabled={busy || !courses.length}>
                Create assessment
              </button>
            </form>
          </Panel>
        </div>
      )}
      {module === "calendar" && (
        <div className="two-column">
          <div className="module-stack">
            <CalendarWidget large events={rows as Event[]} />
            <FacultyScheduleEditor />
          </div>
          <Panel title="Add academic event">
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(
                  () => api.post("/faculty/calendar", f),
                  "Event added to your calendar",
                );
              }}
            >
              <label>
                Event title
                <input name="title" maxLength={255} required />
              </label>
              <label>
                Date
                <input type="date" name="eventDate" required />
              </label>
              <button className="primary" disabled={busy}>
                Add event
              </button>
            </form>
          </Panel>
        </div>
      )}
      {(module === "attendance" || module === "reports") && (
        <div className="module-stack">
          {module === "attendance" && (
            <Panel title="Open attendance session" icon={<QrCode size={21} />}>
              <p className="muted">
                A signed-in student must be enrolled in this course to claim
                attendance. Each token expires after 3 minutes. Proximity
                verification is pending.
              </p>
              <form
                className="inline-form"
                onSubmit={(e) => {
                  const f = fields(e);
                  action(async () => {
                    const r = await api.post("/faculty/attendance/sessions", {
                      sessionId: Number(f.sessionId),
                    });
                    setAttendanceToken(r.data);
                  }, "Attendance session opened");
                }}
              >
                <label>
                  Class session
                  <select name="sessionId" required>
                    {sessions.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.course} ·{" "}
                        {new Date(s.startsAt).toLocaleString("en-IN")} ·{" "}
                        {s.status}
                      </option>
                    ))}
                  </select>
                </label>
                <button className="primary" disabled={busy || !sessions.length}>
                  Generate attendance QR
                </button>
              </form>
              {attendanceToken && (
                <div className="attendance-token">
                  <span
                    className={"badge " + (seconds ? "completed" : "cancelled")}
                  >
                    {seconds
                      ? `Active · ${seconds}s remaining`
                      : "Expired — generate a new token"}
                  </span>
                  {qrImage && seconds > 0 && (
                    <img
                      src={qrImage}
                      alt="Short-lived attendance QR for authenticated enrolled students"
                    />
                  )}
                  <small>
                    Session #{attendanceToken.sessionId} · Student identity is
                    checked on the server.
                  </small>
                </div>
              )}
            </Panel>
          )}
          <Panel title="Attendance records">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Course</th>
                    <th>Session</th>
                    <th>Recorded</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td>{r.student}</td>
                      <td>{r.course}</td>
                      <td>#{r.sessionId}</td>
                      <td>{new Date(r.recordedAt).toLocaleString("en-IN")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!rows.length && <Empty>No attendance recorded yet.</Empty>}
            </div>
          </Panel>
        </div>
      )}
      {module === "settings" && (
        <div className="two-column">
          <Panel title="Your profile">
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(async () => {
                  await api.put("/faculty/settings/profile", {
                    name: f.name,
                    notificationsEnabled: f.notificationsEnabled === "on",
                  });
                  setUser((await api.get("/auth/me")).data);
                }, "Profile updated");
              }}
            >
              <label>
                Full name
                <input
                  name="name"
                  required
                  maxLength={100}
                  defaultValue={user?.name}
                />
              </label>
              <label>
                Institute ID
                <input disabled value={user?.identifier || ""} />
              </label>
              <label className="checkbox">
                <input
                  type="checkbox"
                  name="notificationsEnabled"
                  defaultChecked={user?.notificationsEnabled}
                />{" "}
                Enable notification preferences
              </label>
              <button className="primary" disabled={busy}>
                Save changes
              </button>
            </form>
          </Panel>
          <Panel title="Change password">
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(
                  async () => {
                    await api.put("/faculty/settings/password", f);
                    await refreshCsrf();
                    setUser(null);
                    go("/login/faculty");
                  },
                  "Password changed. Please sign in again.",
                  false,
                );
              }}
            >
              <label>
                Current password
                <input
                  type="password"
                  name="currentPassword"
                  autoComplete="current-password"
                  required
                />
              </label>
              <label>
                New password
                <input
                  type="password"
                  name="newPassword"
                  autoComplete="new-password"
                  minLength={12}
                  maxLength={128}
                  required
                />
              </label>
              <p className="muted">
                Use at least 12 characters. Changing your password signs you
                out.
              </p>
              <button className="primary" disabled={busy}>
                Update password
              </button>
            </form>
          </Panel>
        </div>
      )}
    </main>
  );
}
function CourseSelect({ courses }: { courses: Row[] }) {
  return (
    <label>
      Course
      <select name="courseId" required>
        {courses.map((c) => (
          <option value={c.id} key={c.id}>
            {c.title} · {c.section}
          </option>
        ))}
      </select>
    </label>
  );
}
