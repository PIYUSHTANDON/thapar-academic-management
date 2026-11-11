import { useState, useEffect } from "react";
import { NavLink, Outlet, Link, useNavigate } from "react-router-dom";
import {
  Home,
  MessageSquare,
  ClipboardCheck,
  BookOpen,
  FilePenLine,
  ChartNoAxesCombined,
  CalendarDays,
  Users,
  Settings,
  Menu,
  Search,
  Bell,
  ChevronDown,
  LogOut,
  X,
  UserRound,
} from "lucide-react";
import logo from "../assets/thapar-logo.webp";
import { useAuth } from "../hooks/useAuth";
import { api, errorText } from "../services/api";
const nav = [
  ["dashboard", "Dashboard", Home],
  ["messages", "Messages", MessageSquare],
  ["attendance", "Attendance", ClipboardCheck],
  ["courses", "Courses & LMS", BookOpen],
  ["assessments", "Assessments", FilePenLine],
  ["calendar", "Calendar", CalendarDays],
  ["results", "Results", ChartNoAxesCombined],
  ["settings", "Settings", Settings],
] as const;
export default function StudentLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchOpen, setSearchOpen] = useState(false);
  const [compact, setCompact] = useState(window.innerWidth <= 1000);
  const [collapsed, setCollapsed] = useState(false),
    [mobile, setMobile] = useState(false),
    [menu, setMenu] = useState(""),
    [query, setQuery] = useState(""),
    [results, setResults] = useState<any[]>([]),
    [notes, setNotes] = useState<any[]>([]),
    [error, setError] = useState("");
  useEffect(() => {
    const media = window.matchMedia("(max-width: 1000px)");
    const resize = () => {
      setCompact(media.matches);
      if (!media.matches) setMobile(false);
    };
    const escape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMobile(false);
        setMenu("");
        setSearchOpen(false);
      }
    };
    media.addEventListener("change", resize);
    document.addEventListener("keydown", escape);
    return () => {
      media.removeEventListener("change", resize);
      document.removeEventListener("keydown", escape);
    };
  }, []);
  useEffect(() => {
    api
      .get("/student/notifications")
      .then((r) => setNotes(r.data))
      .catch(() => {});
  }, []);
  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }
    let live = true;
    const t = setTimeout(() => {
      api
        .get("/student/search", { params: { q: query } })
        .then((r) => {
          if (live) setResults(r.data);
        })
        .catch((e) => setError(errorText(e)));
    }, 250);
    return () => {
      live = false;
      clearTimeout(t);
    };
  }, [query]);
  async function signout() {
    try {
      await logout();
      navigate("/");
    } catch (e) {
      setError(errorText(e));
    }
  }
  const initials = user?.name
    .replace(/\(.*\)/, "")
    .split(" ")
    .filter((x) => x !== "Dr.")
    .slice(0, 2)
    .map((x) => x[0])
    .join("");
  return (
    <div
      className={
        "app-shell student-shell " +
        (collapsed ? "sidebar-collapsed " : "") +
        (mobile ? "mobile-open" : "")
      }
    >
      {mobile && (
        <button
          className="mobile-scrim"
          aria-label="Close navigation"
          onClick={() => setMobile(false)}
        />
      )}
      <aside className="sidebar" inert={compact && !mobile}>
        <Link to="/student/dashboard" className="sidebar-brand">
          <img src={logo} alt="Thapar Institute of Engineering & Technology" />
        </Link>
        <div className="sidebar-divider" />
        <nav aria-label="Student navigation">
          {nav.map(([path, title, Icon]) => (
            <NavLink
              key={path}
              to={"/student/" + path}
              title={collapsed ? title : undefined}
              onClick={() => setMobile(false)}
            >
              <Icon size={21} />
              <span>{title}</span>
              {path === "messages" && <i className="nav-dot" />}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="campus-line" aria-hidden="true">
            TIET <span>EST. 1956</span>
          </div>
          <Link className="faculty-profile" to="/student/settings">
            <span className="avatar">{initials}</span>
            <div>
              <strong>{user?.name}</strong>
              <small>Student</small>
            </div>
            <ChevronDown size={16} />
          </Link>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <button
            className="icon-button desktop-toggle"
            aria-label="Toggle sidebar"
            onClick={() => setCollapsed(!collapsed)}
          >
            <Menu />
          </button>
          <button
            className="icon-button mobile-toggle"
            aria-label="Open navigation"
            onClick={() => setMobile(true)}
          >
            <Menu />
          </button>
          <div className="topbar-title">
            <strong>Academic Management System</strong>
            <span>Thapar Institute of Engineering & Technology</span>
          </div>
          <div className="header-tools">
            <button
              className="icon-button mobile-search-toggle"
              aria-label={searchOpen ? "Close search" : "Open search"}
              aria-expanded={searchOpen}
              onClick={() => setSearchOpen(!searchOpen)}
            >
              <Search size={20} />
            </button>
            <div className={"search-wrap " + (searchOpen ? "search-open" : "")}>
              <Search size={20} />
              <input
                aria-label="Search courses and resources"
                placeholder="Search courses, resources…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              {query && (
                <button aria-label="Clear search" onClick={() => setQuery("")}>
                  <X size={15} />
                </button>
              )}
              {query && (
                <div className="dropdown search-results">
                  <small>SEARCH RESULTS</small>
                  {results.map((r, i) => (
                    <Link
                      key={i}
                      to={r.path}
                      onClick={() => {
                        setQuery("");
                        setSearchOpen(false);
                      }}
                    >
                      <span>{r.label}</span>
                      <small>{r.kind}</small>
                    </Link>
                  ))}
                  {!results.length && <p>No matching courses or students.</p>}
                </div>
              )}
            </div>
            <div className="dropdown-anchor">
              <button
                className="icon-button notification-button"
                aria-label="Notifications"
                aria-expanded={menu === "notifications"}
                onClick={() =>
                  setMenu(menu === "notifications" ? "" : "notifications")
                }
              >
                <Bell size={23} />
                {notes.some((n) => !n.read) && (
                  <b>{notes.filter((n) => !n.read).length}</b>
                )}
              </button>
              {menu === "notifications" && (
                <div className="dropdown notifications">
                  <h3>Notifications</h3>
                  {notes.length ? (
                    notes.map((n) => (
                      <button
                        key={n.id}
                        className={n.read ? "read" : ""}
                        onClick={async () => {
                          try {
                            await api.post(
                              "/student/notifications/" + n.id + "/read",
                            );
                            setNotes(
                              notes.map((v) =>
                                v.id === n.id ? { ...v, read: true } : v,
                              ),
                            );
                          } catch (e) {
                            setError(errorText(e));
                          }
                        }}
                      >
                        {n.title}
                        <small>{n.read ? "Read" : "Click to mark read"}</small>
                      </button>
                    ))
                  ) : (
                    <p>You’re all caught up.</p>
                  )}
                </div>
              )}
            </div>
            <div className="dropdown-anchor">
              <button
                className="profile-trigger"
                aria-label="Open profile menu"
                aria-expanded={menu === "profile"}
                onClick={() => setMenu(menu === "profile" ? "" : "profile")}
              >
                <span className="avatar dark">{initials}</span>
                <ChevronDown size={17} />
              </button>
              {menu === "profile" && (
                <div className="dropdown profile-menu">
                  <strong>{user?.name}</strong>
                  <small>{user?.identifier}</small>
                  <Link to="/student/settings" onClick={() => setMenu("")}>
                    <UserRound size={17} /> Profile & settings
                  </Link>
                  <button onClick={signout}>
                    <LogOut size={17} /> Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        {error && (
          <div role="alert" className="global-error">
            {error}
            <button aria-label="Dismiss error" onClick={() => setError("")}>
              <X size={16} />
            </button>
          </div>
        )}
        <Outlet />
      </div>
    </div>
  );
}
