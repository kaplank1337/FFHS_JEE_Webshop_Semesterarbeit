import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { api, type User } from "./api";

type AuthCtx = {
  token: string | null; user: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void; setUser: (u: User | null) => void;
};
const Ctx = createContext<AuthCtx | null>(null);

// Token wird gemäss Schnittstellen-Doku nur im Arbeitsspeicher gehalten.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const login = useCallback(async (username: string, password: string) => {
    const r = await api.login({ username, password });
    setToken(r.accessToken);
    try { setUser(await api.me(r.accessToken)); } catch { setUser(null); }
  }, []);
  const logout = useCallback(() => { setToken(null); setUser(null); }, []);
  return <Ctx.Provider value={{ token, user, login, logout, setUser }}>{children}</Ctx.Provider>;
}
export function useAuth() { const c = useContext(Ctx); if (!c) throw new Error("AuthProvider fehlt"); return c; }
