import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CalendarCheck,
  GraduationCap,
  ClipboardList,
  Layers,
  Zap,
  CalendarDays,
  Upload,
  ChartNoAxesCombined,
  Megaphone,
  ChevronRight,
  UserRound,
  BookOpen,
  Pencil,
} from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { api, errorText } from "../../services/api";
import {
  Panel,
  Empty,
  TextLink,
  CalendarWidget,
  dateKey,
  type Event,
} from "../../components/ui";
import { AnnouncementDialog, StatusBadge } from "./student-ui";
export default function StudentDashboard() {
  const { user } = useAuth(),
    go = useNavigate();
  const [data, setData] = useState<any>(null),
    [profile, setProfile] = useState<any>(null),
    [schedule, setSchedule] = useState<any[]>([]),
    [deadlines, setDeadlines] = useState<any[]>([]),
    [announcements, setAnnouncements] = useState<any[]>([]),
    [events, setEvents] = useState<Event[]>([]),
    [detail, setDetail] = useState<any>(null),
    [error, setError] = useState("");
  async function load() {
    setError("");
    try {
      const [d, p, s, a, n, e] = await Promise.all(
        [
          "dashboard",
          "profile",
          "schedule",
          "assessments",
          "announcements",
          "calendar",
        ].map((path) => api.get("/student/" + path)),
      );
      setData(d.data);
      setProfile(p.data);
      setSchedule(s.data);
      setDeadlines(a.data);
      setAnnouncements(n.data);
      setEvents(e.data);
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, []);
  const today = dateKey(new Date());
  const classes = schedule.filter((s) => s.startsAt.startsWith(today));
  const upcoming = deadlines.filter((a) => a.status !== "Graded").slice(0, 4);
  const initials = profile?.name
    ?.split(" ")
    .slice(0, 2)
    .map((n: string) => n[0])
    .join("");
  return (
    <main className="student-dashboard">
      <section className="welcome-hero">
        <div className="hero-copy">
          <p className="institute-line">Thapar Institute</p>
          <h1>
            Welcome Back, <strong>{user?.name.split(" ")[0]}!</strong>
          </h1>
          <p>
            Track your academics, classes, assignments, and progress in one
            place.
          </p>
          <div className="red-rule" />
        </div>
        <div className="stats-grid">
          {[
            [
              CalendarCheck,
              "Overall Attendance",
              data
                ? data.overallAttendance === null
                  ? "Not available"
                  : data.overallAttendance + "%"
                : "—",
            ],
            [
              GraduationCap,
              "Current CGPA",
              data?.currentCgpa ?? (data ? "Not available" : "—"),
            ],
            [
              ClipboardList,
              "Pending Assignments",
              data?.pendingAssignments ?? "—",
            ],
            [Layers, "Registered Courses", data?.registeredCourses ?? "—"],
          ].map(([Icon, label, value]) => {
            const I = Icon as typeof CalendarCheck;
            return (
              <div className="stat-card" key={String(label)}>
                <span className="stat-icon">
                  <I size={33} />
                </span>
                <div>
                  <strong
                    className={
                      String(value) === "Not available" ? "not-available" : ""
                    }
                  >
                    {String(value)}
                  </strong>
                  <span>{String(label)}</span>
                </div>
              </div>
            );
          })}
        </div>
      </section>
      {error && (
        <div className="error dashboard-error" role="alert">
          {error}
          <button onClick={load}>Retry loading</button>
        </div>
      )}
      {!data && !error && (
        <p className="loading" role="status">
          Loading your student workspace…
        </p>
      )}
      <div className="student-dashboard-grid">
        <div className="student-dashboard-left">
          <Panel
            title="Quick Actions"
            icon={<Zap className="accent" fill="currentColor" />}
          >
            <div className="student-quick-actions">
              {[
                ["View Timetable", "calendar", CalendarDays],
                ["Submit Assignment", "assessments", Upload],
                ["Check Results", "results", ChartNoAxesCombined],
              ].map(([title, path, Icon]) => {
                const I = Icon as typeof CalendarDays;
                return (
                  <button
                    key={String(path)}
                    onClick={() => go("/student/" + path)}
                  >
                    <I size={28} />
                    <span>{String(title)}</span>
                  </button>
                );
              })}
            </div>
          </Panel>
          <div className="student-middle">
            <Panel
              title="Today’s Schedule"
              icon={<CalendarDays className="accent" />}
              action={
                <TextLink onClick={() => go("/student/calendar")}>
                  View Full Timetable
                </TextLink>
              }
            >
              <div className="table-scroll">
                <table className="student-table schedule-table">
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Course</th>
                      <th>Room</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {classes.map((s) => (
                      <tr key={s.id}>
                        <td data-label="Time">
                          {s.startsAt.slice(11, 16)} – {s.endsAt.slice(11, 16)}
                        </td>
                        <td data-label="Course">{s.course}</td>
                        <td data-label="Room">{s.room}</td>
                        <td data-label="Status">
                          <StatusBadge status={s.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!classes.length && <Empty>No classes scheduled today.</Empty>}
              </div>
            </Panel>
            <Panel
              title="Upcoming Deadlines"
              icon={<ClipboardList className="accent" />}
              action={
                <TextLink onClick={() => go("/student/assessments")}>
                  View All
                </TextLink>
              }
            >
              <div className="table-scroll">
                <table className="student-table deadline-table">
                  <thead>
                    <tr>
                      <th>Assignment</th>
                      <th>Course</th>
                      <th>Due Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {upcoming.map((a) => (
                      <tr key={a.id} onClick={() => go("/student/assessments")}>
                        <td>
                          <button
                            className="deadline-link"
                            onClick={() => go("/student/assessments")}
                          >
                            {a.title}
                          </button>
                        </td>
                        <td>{a.course}</td>
                        <td>
                          {new Date(a.deadline).toLocaleDateString("en-IN", {
                            day: "numeric",
                            month: "short",
                          })}
                        </td>
                        <td>
                          <StatusBadge status={a.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!upcoming.length && (
                  <Empty>No upcoming assessment deadlines.</Empty>
                )}
              </div>
            </Panel>
          </div>
          <Panel
            title="Recent Announcements"
            icon={<Megaphone className="accent" />}
            action={
              <TextLink onClick={() => go("/student/courses")}>
                View All
              </TextLink>
            }
          >
            <div className="student-announcements">
              {announcements.slice(0, 4).map((a) => (
                <button key={a.id} onClick={() => setDetail(a)}>
                  <span className="action-icon">
                    <Megaphone size={20} />
                  </span>
                  <span>
                    <strong>{a.title}</strong>
                    <small>
                      {a.description || a.course + " · " + a.faculty}
                    </small>
                  </span>
                  <time>
                    {a.publishedAt
                      ? new Date(a.publishedAt).toLocaleDateString("en-IN", {
                          day: "numeric",
                          month: "short",
                          year: "numeric",
                        })
                      : "Date not available"}
                  </time>
                  <ChevronRight size={17} />
                </button>
              ))}
              {!announcements.length && (
                <Empty>No announcements from your enrolled courses yet.</Empty>
              )}
            </div>
          </Panel>
        </div>
        <div className="student-dashboard-right">
          <section className="panel student-profile-card">
            <div className="student-profile-heading">
              <span className="student-profile-avatar">
                {initials || <UserRound />}
              </span>
              <div>
                <h2>{profile?.name || user?.name}</h2>
                <p>Roll No. {profile?.enrollmentNumber ?? "Not available"}</p>
              </div>
              <button
                aria-label="Student profile settings"
                onClick={() => go("/student/settings")}
              >
                <Pencil size={17} />
              </button>
            </div>
            <div className="student-profile-details">
              <p>
                <GraduationCap size={22} />
                <span>{profile?.program ?? "Program not available"}</span>
              </p>
              <p>
                <Layers size={21} />
                <span>
                  Year {profile?.academicYear ?? "Not available"} · Semester{" "}
                  {profile?.semester ?? "Not available"}
                </span>
              </p>
              <p>
                <BookOpen size={20} />
                <span>
                  Roll No. {profile?.enrollmentNumber ?? "Not available"}
                </span>
              </p>
            </div>
            {profile?.synthetic && (
              <small className="synthetic-note">
                Synthetic local demo record
              </small>
            )}
          </section>
          <CalendarWidget events={events} />
        </div>
      </div>
      {detail && (
        <AnnouncementDialog
          announcement={detail}
          onClose={() => setDetail(null)}
        />
      )}
    </main>
  );
}
