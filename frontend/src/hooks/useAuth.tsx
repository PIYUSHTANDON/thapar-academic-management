import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from "react";
import { api, refreshCsrf } from "../services/api";
export type User = {
  id: number;
  name: string;
  identifier: string;
  role: "FACULTY" | "STUDENT" | "ADMIN";
  notificationsEnabled: boolean;
};
const Context = createContext<{
  user: User | null;
  loading: boolean;
  setUser: (u: User | null) => void;
  logout: () => Promise<void>;
}>({ user: null, loading: true, setUser: () => {}, logout: async () => {} });
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    api
      .get("/auth/session")
      .then((r) => setUser(r.data.authenticated ? r.data.user : null))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);
  async function logout() {
    await api.post("/auth/logout");
    setUser(null);
    await refreshCsrf();
  }
  return (
    <Context.Provider value={{ user, setUser, loading, logout }}>
      {children}
    </Context.Provider>
  );
}
export const useAuth = () => useContext(Context);
