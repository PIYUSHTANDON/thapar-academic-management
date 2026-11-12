import { useState } from "react";
import { Outlet, Link, useNavigate } from "react-router-dom";
import {
  Home,
  MessageSquare,
  ClipboardCheck,
  BookOpen,
  FilePenLine,
  CalendarDays,
  Users,
  Settings,
  ChevronDown,
  LogOut,
  X,
  UserRound,
} from "lucide-react";
import { motion } from "framer-motion";
import logoDark from "../assets/thapar-logo-dark.png";
import crestDark from "../assets/thapar-crest-dark.png";
import { useAuth } from "../hooks/useAuth";
import { errorText } from "../services/api";
import {
  Sidebar,
  SidebarBody,
  SidebarLink,
  useSidebar,
} from "../components/ui/sidebar";

const nav = [
  ["dashboard", "Dashboard", Home],
  ["messages", "Messages", MessageSquare],
  ["attendance", "Attendance", ClipboardCheck],
  ["courses", "Courses & LMS", BookOpen],
  ["assessments", "Assessments", FilePenLine],
  ["calendar", "Calendar", CalendarDays],
  ["students", "Student Roster", Users],
  ["settings", "Settings", Settings],
] as const;

function FacultySidebarInner({
  user,
  initials,
  menu,
  setMenu,
  signout,
}: {
  user: any;
  initials: string;
  menu: string;
  setMenu: (val: string) => void;
  signout: () => void;
}) {
  const { open } = useSidebar();

  return (
    <div className="sidebar-content-inner">
      <div className="sidebar-brand-wrapper">
        <Link to="/faculty/dashboard" className="sidebar-brand-link">
          {open ? (
            <motion.img
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.18 }}
              src={logoDark}
              alt="Thapar Institute"
              className="sidebar-logo-expanded"
            />
          ) : (
            <img
              src={crestDark}
              alt="Thapar Institute"
              className="sidebar-logo-collapsed"
            />
          )}
        </Link>
      </div>

      <div className="sidebar-divider" />

      <nav className="sidebar-nav-list" aria-label="Faculty navigation">
        {nav.map(([path, title, Icon]) => (
          <SidebarLink
            key={path}
            link={{
              label: title,
              href: "/faculty/" + path,
              icon: <Icon size={21} />,
              dot: path === "messages",
            }}
          />
        ))}
      </nav>

      <div className="sidebar-bottom">
        <div className="sidebar-profile-wrap">
          <button
            className={`faculty-profile sidebar-profile-btn ${
              open ? "is-expanded" : "is-collapsed"
            }`}
            aria-label="Open profile menu"
            aria-expanded={menu === "profile"}
            onClick={() => setMenu(menu === "profile" ? "" : "profile")}
          >
            <span className="avatar">{initials}</span>
            {open && (
              <motion.div
                initial={{ opacity: 0, x: -6 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.15 }}
                className="sidebar-profile-details"
              >
                <strong>{user?.name}</strong>
                <small>Faculty · CSE</small>
              </motion.div>
            )}
            {open && <ChevronDown size={16} className="profile-chevron" />}
          </button>
          {menu === "profile" && (
            <div className="dropdown profile-menu sidebar-profile-menu">
              <strong>{user?.name}</strong>
              <small>{user?.identifier}</small>
              <Link to="/faculty/settings" onClick={() => setMenu("")}>
                <UserRound size={17} /> Profile & settings
              </Link>
              <button onClick={signout}>
                <LogOut size={17} /> Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function FacultyLayoutContent({
  user,
  initials,
  menu,
  setMenu,
  signout,
  error,
  setError,
}: {
  user: any;
  initials: string;
  menu: string;
  setMenu: (val: string) => void;
  signout: () => void;
  error: string;
  setError: (val: string) => void;
}) {
  const { open } = useSidebar();

  return (
    <div className={`app-shell ${open ? "sidebar-open" : "sidebar-closed"}`}>
      <SidebarBody>
        <FacultySidebarInner
          user={user}
          initials={initials}
          menu={menu}
          setMenu={setMenu}
          signout={signout}
        />
      </SidebarBody>
      <motion.div
        className="main-shell"
        animate={{
          marginLeft: open ? "240px" : "72px",
        }}
        transition={{
          duration: 0.22,
          ease: [0.4, 0, 0.2, 1],
        }}
      >
        {error && (
          <div role="alert" className="global-error">
            {error}
            <button aria-label="Dismiss error" onClick={() => setError("")}>
              <X size={16} />
            </button>
          </div>
        )}
        <Outlet />
      </motion.div>
    </div>
  );
}

export default function FacultyLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menu, setMenu] = useState("");
  const [error, setError] = useState("");

  async function signout() {
    try {
      await logout();
      navigate("/");
    } catch (e) {
      setError(errorText(e));
    }
  }

  const initials = user?.name
    ? user.name
        .replace(/\(.*\)/, "")
        .split(" ")
        .filter((x: string) => x !== "Dr.")
        .slice(0, 2)
        .map((x: string) => x[0])
        .join("")
    : "FA";

  return (
    <Sidebar open={menu === "profile" ? true : undefined}>
      <FacultyLayoutContent
        user={user}
        initials={initials}
        menu={menu}
        setMenu={setMenu}
        signout={signout}
        error={error}
        setError={setError}
      />
    </Sidebar>
  );
}
