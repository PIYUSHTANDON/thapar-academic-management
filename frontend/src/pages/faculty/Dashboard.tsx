import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  Users,
  ChartNoAxesCombined,
  CalendarDays,
  Zap,
  ClipboardCheck,
  FilePenLine,
  ArrowRight,
  ChevronRight,
  CircleAlert,
  FileText,
  QrCode,
} from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { api, errorText } from "../../services/api";
import {
  Panel,
  CalendarWidget,
  TextLink,
  Empty,
  dateKey,
  type Event,
} from "../../components/ui";
export default function Dashboard() {
  const { user } = useAuth();
  const go = useNavigate();
  const [data, setData] = useState<any>(null),
    [schedule, setSchedule] = useState<any[]>([]),
    [events, setEvents] = useState<Event[]>([]),
    [error, setError] = useState("");
  async function load() {
    setError("");
    try {
      const [d, s, e] = await Promise.all([
        api.get("/faculty/dashboard"),
        api.get("/faculty/schedule"),
        api.get("/faculty/calendar"),
      ]);
      setData(d.data);
      setSchedule(s.data);
      setEvents(e.data);
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, []);
  const today = dateKey(new Date()),
    todaySchedule = schedule.filter((s) => s.startsAt.startsWith(today));
  const quick = [
    ["Take Attendance", "attendance", ClipboardCheck],
    ["Create Assessment", "assessments", FilePenLine],
    ["View Reports", "reports", ChartNoAxesCombined],
    ["Manage Courses", "courses", Users],
  ] as const;
  return (
    <main className="dashboard">
      <section className="welcome-hero">
        <div className="hero-copy">
          <p className="institute-line">Thapar Institute</p>
          <h1>
            Welcome Back,
            <br />
            <strong>{user?.name}!</strong>
          </h1>
          <p>Together towards excellence.</p>
          <div className="red-rule" />
        </div>
        <div className="stats-grid">
          {[
            [BookOpen, "Active Classes", data?.activeClasses],
            [Users, "Total Students", data?.totalStudents],
            [
              ChartNoAxesCombined,
              "Attendance",
              data ? data.attendancePercentage + "%" : undefined,
            ],
            [CalendarDays, "Pending Reviews", data?.pendingReviews],
          ].map(([Icon, label, value]) => {
            const I = Icon as typeof BookOpen;
            return (
              <div className="stat-card" key={String(label)}>
                <span className="stat-icon">
                  <I size={32} />
                </span>
                <div>
                  <strong>
                    {value === undefined ? "—" : String(value).padStart(2, "0")}
                  </strong>
                  <span>{String(label)}</span>
                </div>
              </div>
            );
          })}
        </div>
      </section>
      {error ? (
        <div className="error dashboard-error" role="alert">
          {error}
          <button onClick={load}>Retry loading</button>
        </div>
      ) : (
        !data && (
          <p className="loading" role="status">
            Loading your faculty workspace…
          </p>
        )
      )}
      <div className="dashboard-grid">
        <div className="dashboard-left">
          <Panel
            title="Quick Access"
            icon={<Zap className="accent" fill="currentColor" />}
            action={
              <TextLink onClick={() => go("/faculty/courses")}>
                View All
              </TextLink>
            }
          >
            <div className="quick-grid">
              {quick.map(([label, path, Icon]) => (
                <button key={path} onClick={() => go("/faculty/" + path)}>
                  <Icon size={26} />
                  <span>{label}</span>
                </button>
              ))}
            </div>
          </Panel>
          <Panel
            title="Today’s Teaching Schedule"
            icon={<CalendarDays className="accent" />}
            action={
              <TextLink onClick={() => go("/faculty/calendar")}>
                View Full Calendar
              </TextLink>
            }
          >
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Course</th>
                    <th>Room</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {todaySchedule.map((s) => (
                    <tr key={s.id}>
                      <td>
                        {s.startsAt.slice(11, 16)} – {s.endsAt.slice(11, 16)}
                      </td>
                      <td>
                        <strong>{s.course}</strong>
                      </td>
                      <td data-label="Room">{s.room}</td>
                      <td>
                        <span className={"badge " + s.status.toLowerCase()}>
                          <i />
                          {s.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!todaySchedule.length && (
                <Empty>
                  {data
                    ? "No classes scheduled today. Enjoy time for research and preparation."
                    : "Your teaching schedule will appear here."}
                </Empty>
              )}
            </div>
          </Panel>
          <div className="workspace-footer">
            <span>ACADEMIC WORKSPACE</span>
            <p>Every great future begins with a great teacher.</p>
            <span>TIET · PATIALA</span>
          </div>
        </div>
        <div className="dashboard-right">
          <CalendarWidget events={events} />
          <Panel
            title="Action Required"
            icon={<CircleAlert className="accent" />}
            action={
              <TextLink onClick={() => go("/faculty/assessments")}>
                View All
              </TextLink>
            }
          >
            <div className="action-list">
              {data?.actions?.map((a: any, i: number) => {
                const Icon = i === 1 ? QrCode : FileText;
                return (
                  <button key={i} onClick={() => go(a.path)}>
                    <span className="action-icon">
                      <Icon size={19} />
                    </span>
                    <span>
                      <strong>{a.title}</strong>
                      <small>
                        {a.course} · {a.status}
                      </small>
                    </span>
                    <ChevronRight size={17} />
                  </button>
                );
              })}
              {!data?.actions?.length && (
                <Empty>No pending academic actions.</Empty>
              )}
            </div>
          </Panel>
        </div>
      </div>
    </main>
  );
}
