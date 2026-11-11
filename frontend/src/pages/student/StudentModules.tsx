import { useEffect, useState, useCallback, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Send,
  Download,
  BookOpen,
  Check,
  ArrowLeft,
  GraduationCap,
} from "lucide-react";
import { Panel, Empty, CalendarWidget, type Event } from "../../components/ui";
import { api, errorText, refreshCsrf } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import QrScanner, { parseAttendanceToken } from "./QrScanner";
import { AnnouncementDialog, StatusBadge } from "./student-ui";
export default function StudentModules() {
  const module = useLocation().pathname.split("/")[2],
    go = useNavigate(),
    { user, setUser, logout } = useAuth();
  const [courses, setCourses] = useState<any[]>([]),
    [profile, setProfile] = useState<any>(null),
    [data, setData] = useState<any>(null),
    [contacts, setContacts] = useState<any[]>([]),
    [messages, setMessages] = useState<any[]>([]),
    [course, setCourse] = useState(""),
    [recipient, setRecipient] = useState(""),
    [resources, setResources] = useState<any[]>([]),
    [schedule, setSchedule] = useState<any[]>([]),
    [token, setToken] = useState(""),
    [announcement, setAnnouncement] = useState<any>(null),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true);
  const titles: Record<string, string> = {
    messages: "Messages",
    attendance: "Attendance",
    courses: "Courses & LMS",
    assessments: "Assessments",
    calendar: "Academic Calendar",
    results: "Results",
    settings: "Settings",
  };
  async function load() {
    setLoading(true);
    setError("");
    try {
      const [c, p] = await Promise.all([
        api.get("/student/courses"),
        api.get("/student/profile"),
      ]);
      setCourses(c.data);
      setProfile(p.data);
      if (!course && c.data.length) setCourse(String(c.data[0].id));
      if (["attendance", "assessments", "calendar", "results"].includes(module))
        setData((await api.get("/student/" + module)).data);
      if (module === "messages") {
        const [teachers, history] = await Promise.all([
          api.get("/student/messages/contacts"),
          api.get("/student/messages"),
        ]);
        setContacts(teachers.data);
        setMessages(history.data);
        if (!recipient && teachers.data.length)
          setRecipient(String(teachers.data[0].id));
      }
      if (module === "courses")
        setSchedule((await api.get("/student/schedule")).data);
      return true;
    } catch (e) {
      setError(errorText(e));
      return false;
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    setData(null);
    setToast("");
    setToken("");
    load();
  }, [module]);
  useEffect(() => {
    if (module === "courses" && course) {
      setResources([]);
      api
        .get("/student/courses/" + course + "/resources")
        .then((r) => setResources(r.data))
        .catch((e) => setError(errorText(e)));
    }
  }, [course, module, toast]);
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
  const decoded = useCallback((value: string) => {
    setToken(value);
    setToast("Attendance QR decoded. Review and claim below.");
  }, []);
  return (
    <main className="module-page student-module" aria-busy={loading}>
      <div className="module-heading">
        <div>
          <p className="eyebrow">STUDENT WORKSPACE</p>
          <h1>{titles[module] || "Student workspace"}</h1>
          <p className="muted">
            {module === "messages"
              ? "Connect with the teachers of your enrolled courses."
              : module === "attendance"
                ? "Your attendance records and secure class attendance claims."
                : module === "assessments"
                  ? "Stay on top of deadlines, submissions, and published feedback."
                  : module === "results"
                    ? "Published assessment marks and official academic information."
                    : "Your academics, connected in one place."}
          </p>
        </div>
        <button className="back-link" onClick={() => go("/student/dashboard")}>
          <ArrowLeft size={16} />
          Dashboard
        </button>
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
          Loading your student workspace…
        </p>
      )}
      {module === "messages" && (
        <div className="message-layout">
          <Panel title="Your teachers">
            <div className="conversation-list">
              {contacts.map((c) => (
                <button
                  key={c.id}
                  className={recipient === String(c.id) ? "active" : ""}
                  onClick={() => setRecipient(String(c.id))}
                >
                  <span className="avatar">
                    {c.name
                      .split(" ")
                      .map((n: string) => n[0])
                      .slice(0, 2)
                      .join("")}
                  </span>
                  <span>
                    <strong>{c.name}</strong>
                    <small>Course faculty</small>
                  </span>
                </button>
              ))}
              {!contacts.length && <Empty>No course faculty available.</Empty>}
            </div>
          </Panel>
          <Panel
            title={
              contacts.find((c) => String(c.id) === recipient)?.name ||
              "Select your teacher"
            }
          >
            <div className="message-history">
              {messages
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
            </div>
            <form
              className="compose"
              onSubmit={(e) => {
                const f = fields(e),
                  form = e.currentTarget;
                action(async () => {
                  await api.post("/student/messages", {
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
                maxLength={4000}
                required
                placeholder="Write to your teacher…"
              />
              <button
                className="primary"
                aria-label="Send message"
                disabled={busy || !recipient}
              >
                <Send size={18} />
              </button>
            </form>
          </Panel>
        </div>
      )}
      {module === "attendance" && (
        <div className="module-stack">
          <Panel title="Claim class attendance">
            <p className="muted">
              Scan your Faculty attendance QR or enter its short-lived token.
              Your identity and enrollment are checked by the server.
              Location/proximity verification is not implemented.
            </p>
            <QrScanner onToken={decoded} />
            <form
              className="stack-form"
              onSubmit={(e) => {
                e.preventDefault();
                action(async () => {
                  let parsed;
                  try {
                    parsed = parseAttendanceToken(token);
                  } catch (e) {
                    throw e;
                  }
                  await api.post("/student/attendance/claim", {
                    token: parsed,
                  });
                  setToken("");
                }, "Attendance recorded");
              }}
            >
              <label>
                Attendance token
                <textarea
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  required
                  maxLength={1000}
                  placeholder="Paste the QR token or its JSON content"
                />
              </label>
              <button className="primary" disabled={busy}>
                Claim attendance
              </button>
            </form>
          </Panel>
          <Panel title="Subject-wise attendance">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Attended</th>
                    <th>Missed</th>
                    <th>Total</th>
                    <th>Attendance</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.courses?.map((c: any) => (
                    <tr key={c.courseId}>
                      <td>
                        {c.course} · {c.section}
                      </td>
                      <td>{c.attended}</td>
                      <td>{c.missed}</td>
                      <td>{c.total}</td>
                      <td>
                        {c.percentage === null
                          ? "Not available"
                          : c.percentage + "%"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!data?.courses?.length && <Empty>No enrolled courses.</Empty>}
            </div>
          </Panel>
          <Panel title="Class attendance history">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Class</th>
                    <th>Date / time</th>
                    <th>Room</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.records?.map((r: any) => (
                    <tr key={r.sessionId}>
                      <td>{r.course}</td>
                      <td>{new Date(r.startsAt).toLocaleString("en-IN")}</td>
                      <td>{r.room}</td>
                      <td>
                        <StatusBadge status={r.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!data?.records?.length && (
                <Empty>No completed classes to report yet.</Empty>
              )}
            </div>
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
                <p>{c.faculty}</p>
              </button>
            ))}
            {!courses.length && <Empty>No enrolled courses.</Empty>}
          </div>
          {course && (
            <div className="module-stack">
              <Panel title="Course information">
                <p>
                  {courses.find((c) => String(c.id) === course)?.description ||
                    "No course description available."}
                </p>
                <p className="muted">
                  Faculty:{" "}
                  {courses.find((c) => String(c.id) === course)?.faculty}
                </p>
              </Panel>
              <Panel title="Learning resources & announcements">
                {resources.map((r) => (
                  <div className="resource-row" key={r.id}>
                    <span>
                      <small>
                        {r.kind} ·{" "}
                        {r.publishedAt
                          ? new Date(r.publishedAt).toLocaleDateString("en-IN")
                          : "Date not available"}
                      </small>
                      <strong>{r.title}</strong>
                      {r.description && (
                        <p className="muted">{r.description}</p>
                      )}
                    </span>
                    {r.kind === "File" ? (
                      <a
                        aria-label={"Download " + r.title}
                        href={
                          api.defaults.baseURL + "/student/resources/" + r.id
                        }
                      >
                        <Download size={19} />
                      </a>
                    ) : (
                      <button
                        className="text-link"
                        onClick={() => setAnnouncement(r)}
                      >
                        Read announcement
                      </button>
                    )}
                  </div>
                ))}
                {!resources.length && (
                  <Empty>No course resources or announcements yet.</Empty>
                )}
              </Panel>
              <Panel title="Course schedule">
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Date / time</th>
                        <th>Room</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {schedule
                        .filter((v) => String(v.courseId) === course)
                        .map((v) => (
                          <tr key={v.id}>
                            <td>
                              {new Date(v.startsAt).toLocaleString("en-IN")}
                            </td>
                            <td>{v.room}</td>
                            <td>
                              <StatusBadge status={v.status} />
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
            </div>
          )}
        </div>
      )}
      {module === "assessments" && (
        <div className="student-assessments">
          {(Array.isArray(data) ? data : []).map((a: any) => (
            <Panel
              key={a.id}
              title={a.title}
              action={<StatusBadge status={a.status} />}
            >
              <p className="muted">
                {a.course} · {a.kind}
              </p>
              <p className="assessment-deadline">
                Due {new Date(a.deadline).toLocaleString("en-IN")}
              </p>
              {a.submissionId ? (
                <div className="submitted-work">
                  <p>
                    <strong>Submitted:</strong>{" "}
                    {new Date(a.submittedAt).toLocaleString("en-IN")}
                  </p>
                  <p>{a.content}</p>
                  {a.fileName && (
                    <a
                      className="text-link"
                      href={
                        api.defaults.baseURL +
                        "/student/submissions/" +
                        a.submissionId +
                        "/file"
                      }
                    >
                      <Download size={17} />
                      {a.fileName}
                    </a>
                  )}
                  {a.marks !== null ? (
                    <div className="published-feedback">
                      <strong>{a.marks} / 100</strong>
                      <p>{a.feedback || "No faculty feedback provided."}</p>
                    </div>
                  ) : (
                    <p className="muted">
                      Marks and feedback will appear after faculty publication.
                    </p>
                  )}
                </div>
              ) : a.status === "Overdue" ? (
                <Empty>The submission deadline has passed.</Empty>
              ) : (
                <form
                  className="stack-form"
                  onSubmit={(e) => {
                    e.preventDefault();
                    const form = e.currentTarget;
                    const payload = new FormData(form);
                    payload.set("assessmentId", String(a.id));
                    action(async () => {
                      await api.post("/student/submissions", payload);
                    }, "Assignment submitted");
                  }}
                >
                  <label>
                    Submission text
                    <textarea
                      name="content"
                      maxLength={4000}
                      placeholder="Your response (or attach a supported file below)"
                    />
                  </label>
                  <label>
                    Assignment file
                    <input
                      type="file"
                      name="file"
                      accept=".pdf,.txt,application/pdf,text/plain"
                    />
                  </label>
                  <small className="muted">
                    PDF or UTF-8 TXT, up to 5 MB. Submitted work can be
                    downloaded by your course Faculty.
                  </small>
                  <button className="primary" disabled={busy}>
                    Submit assignment
                  </button>
                </form>
              )}
            </Panel>
          ))}
          {!data?.length && !loading && (
            <Empty>No assessments in your enrolled courses.</Empty>
          )}
        </div>
      )}
      {module === "calendar" && (
        <CalendarWidget
          large
          events={(Array.isArray(data) ? data : []) as Event[]}
        />
      )}
      {module === "results" && (
        <div className="module-stack">
          <Panel
            title="Official academic record"
            icon={<GraduationCap className="accent" />}
          >
            <p className="official-cgpa">
              Current CGPA{" "}
              <strong>{data?.officialCgpa ?? "Not available"}</strong>
            </p>
            <p className="muted">
              Official CGPA is shown only when stored in your authorized
              academic record. Assessment marks are not used to calculate CGPA.
            </p>
          </Panel>
          <Panel title="Published assessment results">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Assessment</th>
                    <th>Type</th>
                    <th>Marks / 100</th>
                    <th>Faculty feedback</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.results?.map((r: any) => (
                    <tr key={r.id}>
                      <td>{r.course}</td>
                      <td>{r.title}</td>
                      <td>{r.kind}</td>
                      <td>
                        <strong>{r.marks}</strong>
                      </td>
                      <td className="feedback-cell">
                        {r.feedback || "No feedback provided."}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!data?.results?.length && (
                <Empty>No published results yet.</Empty>
              )}
            </div>
          </Panel>
        </div>
      )}
      {module === "settings" && profile && (
        <div className="two-column">
          <Panel title="Student profile">
            <dl className="student-profile-list">
              {[
                ["Enrollment number", profile.enrollmentNumber],
                ["Program", profile.program],
                ["Academic year", profile.academicYear],
                ["Semester", profile.semester],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt>{label}</dt>
                  <dd>{value ?? "Not available"}</dd>
                </div>
              ))}
            </dl>
            {profile.synthetic && (
              <p className="synthetic-note">Synthetic local demo record</p>
            )}
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(async () => {
                  await api.put("/student/settings/profile", {
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
                  defaultValue={profile.name}
                />
              </label>
              <label className="checkbox">
                <input
                  name="notificationsEnabled"
                  type="checkbox"
                  defaultChecked={profile.notificationsEnabled}
                />
                Enable notification preferences
              </label>
              <button className="primary" disabled={busy}>
                Save changes
              </button>
            </form>
            <button
              className="secondary settings-logout"
              onClick={() =>
                action(
                  async () => {
                    await logout();
                    go("/login/student");
                  },
                  "Signed out",
                  false,
                )
              }
            >
              Sign out
            </button>
          </Panel>
          <Panel title="Change password">
            <form
              className="stack-form"
              onSubmit={(e) => {
                const f = fields(e);
                action(
                  async () => {
                    await api.put("/student/settings/password", f);
                    await refreshCsrf();
                    setUser(null);
                    go("/login/student");
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
                  required
                  autoComplete="current-password"
                  maxLength={128}
                />
              </label>
              <label>
                New password
                <input
                  type="password"
                  name="newPassword"
                  required
                  minLength={12}
                  maxLength={128}
                  autoComplete="new-password"
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
      {announcement && (
        <AnnouncementDialog
          announcement={announcement}
          onClose={() => setAnnouncement(null)}
        />
      )}
    </main>
  );
}
