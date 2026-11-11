import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  Outlet,
  Link,
} from "react-router-dom";
import { AuthProvider, useAuth } from "../hooks/useAuth";
import Login, { RoleSelection } from "../pages/auth/Login";
import FacultyLayout from "../layouts/FacultyLayout";
import Dashboard from "../pages/faculty/Dashboard";
import Modules from "../pages/faculty/Modules";
import StudentLayout from "../layouts/StudentLayout";
import StudentDashboard from "../pages/student/StudentDashboard";
import StudentModules from "../pages/student/StudentModules";
function Protected({ role }: { role: "FACULTY" | "STUDENT" }) {
  const { user, loading } = useAuth();
  if (loading)
    return (
      <div className="full-loading" role="status">
        Opening your academic workspace…
      </div>
    );
  if (!user) return <Navigate to={"/login/" + role.toLowerCase()} replace />;
  if (user.role !== role)
    return (
      <Navigate
        to={
          user.role === "FACULTY"
            ? "/faculty/dashboard"
            : user.role === "STUDENT"
              ? "/student/dashboard"
              : "/access-denied"
        }
        replace
      />
    );
  return <Outlet />;
}
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RoleSelection />} />
          <Route path="/login/:role" element={<Login />} />
          <Route element={<Protected role="FACULTY" />}>
            <Route path="/faculty" element={<FacultyLayout />}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              {[
                "messages",
                "attendance",
                "courses",
                "assessments",
                "calendar",
                "students",
                "settings",
                "reports",
              ].map((path) => (
                <Route key={path} path={path} element={<Modules />} />
              ))}
            </Route>
          </Route>
          <Route element={<Protected role="STUDENT" />}>
            <Route path="/student" element={<StudentLayout />}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<StudentDashboard />} />
              <Route
                path="pending"
                element={<Navigate to="/student/dashboard" replace />}
              />
              {[
                "messages",
                "attendance",
                "courses",
                "assessments",
                "calendar",
                "results",
                "settings",
              ].map((path) => (
                <Route
                  key={path}
                  path={path}
                  element={<StudentModules key={path} />}
                />
              ))}
            </Route>
          </Route>
          <Route
            path="/access-denied"
            element={
              <main className="role-page">
                <div className="role-card">
                  <h1>This portal is for faculty and students.</h1>
                  <Link to="/">Choose a portal</Link>
                </div>
              </main>
            }
          />
          <Route
            path="*"
            element={
              <main className="role-page">
                <div className="role-card">
                  <h1>Page not found</h1>
                  <Link to="/">Return to your portal</Link>
                </div>
              </main>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
