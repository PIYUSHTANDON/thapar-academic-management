import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import {
  Mail,
  LockKeyhole,
  Eye,
  EyeOff,
  ArrowRight,
  GraduationCap,
  BookOpen,
  ShieldCheck,
  CalendarDays,
  Users,
  ClipboardCheck,
  Settings,
  MessageSquare,
  ArrowLeft,
} from "lucide-react";
import logo from "../../assets/thapar-logo.webp";
import { api, errorText, refreshCsrf } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
export function RoleSelection() {
  return (
    <div className="role-page">
      <div className="role-card">
        <img src={logo} alt="Thapar Institute of Engineering & Technology" />
        <p className="eyebrow">ONE CAMPUS. CONNECTED.</p>
        <h1>Academic Management System</h1>
        <p className="muted">
          Your academic life, thoughtfully connected.
          <br />
          Choose your portal to get started.
        </p>
        <div className="role-options">
          <Link to="/login/faculty">
            <BookOpen />
            <strong>Faculty Portal</strong>
            <span>Teach, guide, and inspire.</span>
            <ArrowRight />
          </Link>
          <Link to="/login/student">
            <GraduationCap />
            <strong>Student Portal</strong>
            <span>Your learning journey starts here.</span>
            <ArrowRight />
          </Link>
        </div>
        <small>Thapar Institute of Engineering & Technology · Patiala</small>
      </div>
    </div>
  );
}
export default function Login() {
  const { role } = useParams();
  const faculty = role === "faculty";
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [show, setShow] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [forgot, setForgot] = useState(false);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const f = new FormData(e.currentTarget);
    try {
      const { data } = await api.post("/auth/login", {
        identifier: f.get("identifier"),
        password: f.get("password"),
        rememberMe: f.get("remember") === "on",
        role: faculty ? "FACULTY" : "STUDENT",
      });
      setUser(data);
      await refreshCsrf();
      navigate(faculty ? "/faculty/dashboard" : "/student/dashboard", {
        replace: true,
      });
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  if (role !== "faculty" && role !== "student")
    return <Navigate to="/" replace />;
  return (
    <main className="login-page">
      <section className="login-story">
        <img
          className="story-logo"
          src={logo}
          alt="Thapar Institute of Engineering & Technology"
        />
        <div className="story-copy">
          <p className="institute-line">Academic Management System</p>
          <h1>{faculty ? "Faculty" : "Student"} Portal</h1>
          <p>
            Academic Management System — {faculty ? "Faculty" : "Student"}{" "}
            Access
          </p>
          <div className="red-rule" />
        </div>
        <p className="story-footer">
          A legacy of excellence.
          <br />
          <strong>A future of possibilities.</strong>
        </p>
      </section>
      <section className="login-side">
        <Link className="back-link" to="/">
          <ArrowLeft size={16} /> Choose another portal
        </Link>
        <div className="login-card">
          <header>
            <img src={logo} alt="Thapar logo" />
            <div>
              <strong>Academic Management System</strong>
              <p>Thapar Institute of Engineering & Technology</p>
            </div>
          </header>
          <div className="login-intro">
            <p className="eyebrow">YOUR CAMPUS WORKSPACE</p>
            <h2>
              Welcome Back, <em>{faculty ? "Faculty" : "Student"}</em>
            </h2>
            <p>
              Sign in to{" "}
              {faculty
                ? "manage courses, attendance, assessments, and student records."
                : "access your academic account."}
            </p>
          </div>
          <form onSubmit={submit}>
            <label htmlFor="identifier">
              {faculty
                ? "Employee ID or Official Email"
                : "Enrollment Number or Official Email"}
            </label>
            <div className="input-icon">
              <Mail size={20} />
              <input
                id="identifier"
                name="identifier"
                autoComplete="username"
                required
                maxLength={120}
                placeholder={
                  faculty
                    ? "Employee ID or Official Email"
                    : "Enrollment Number or Official Email"
                }
              />
            </div>
            <label htmlFor="password">Password</label>
            <div className="input-icon">
              <LockKeyhole size={20} />
              <input
                id="password"
                name="password"
                type={show ? "text" : "password"}
                autoComplete="current-password"
                required
                maxLength={128}
                placeholder="Enter your password"
              />
              <button
                type="button"
                aria-label={show ? "Hide password" : "Show password"}
                onClick={() => setShow(!show)}
              >
                {show ? <Eye size={20} /> : <EyeOff size={20} />}
              </button>
            </div>
            <div className="login-options">
              <label className="checkbox">
                <input name="remember" type="checkbox" /> Remember me{" "}
                <span title="Extends this browser session to 8 hours">
                  (8 hours)
                </span>
              </label>
              <button
                type="button"
                className="text-link"
                onClick={() => setForgot(true)}
              >
                Forgot Password?
              </button>
            </div>
            {error && (
              <p className="error" role="alert">
                {error}
              </p>
            )}
            <button className="primary sign-in" disabled={busy}>
              {busy ? "Signing in…" : "Sign In"}
              <ArrowRight size={23} />
            </button>
          </form>
          <div className="secure-note">
            <ShieldCheck size={16} /> Secure institute access
          </div>
        </div>
        <div className="portal-features">
          {[
            [MessageSquare, "Messages"],
            [ClipboardCheck, "Attendance"],
            [BookOpen, "Courses & LMS"],
            [CalendarDays, "Calendar"],
            [Users, "Student Roster"],
            [Settings, "Settings"],
          ].map(([Icon, title]) => {
            const I = Icon as typeof Mail;
            return (
              <span key={String(title)}>
                <I size={21} />
                {String(title)}
              </span>
            );
          })}
        </div>
        <p className="authorized">
          <ShieldCheck size={19} /> For authorized{" "}
          {faculty ? "faculty members" : "students"} only.
        </p>
      </section>
      {forgot && (
        <div className="modal-backdrop">
          <section
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reset-title"
          >
            <h2 id="reset-title">Recover your account</h2>
            <p>
              Password recovery must be handled by the institute IT helpdesk.
              Contact your department or institute IT support to verify your
              identity and request a reset.
            </p>
            <p className="muted">
              Self-service email recovery is not configured for this
              installation.
            </p>
            <button
              autoFocus
              className="primary"
              onClick={() => setForgot(false)}
            >
              Back to sign in
            </button>
          </section>
        </div>
      )}
    </main>
  );
}
