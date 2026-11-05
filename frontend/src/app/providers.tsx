import { useEffect, type ReactNode } from "react";
import { useAuth } from "../features/auth/hooks/useAuth";
import { session } from "../features/auth/services/session";
export function Providers({ children }: { children: ReactNode }) {
  const auth = useAuth();
  useEffect(() => session.bootstrap(), []);
  useEffect(() => {
    if (auth.status !== "authenticated") return;
    const timer = setTimeout(
      () => session.expire(auth.epoch),
      Math.max(0, auth.expiresAt - Date.now()),
    );
    const check = () => {
      if (Date.now() >= auth.expiresAt) session.expire(auth.epoch);
    };
    window.addEventListener("focus", check);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("focus", check);
    };
  }, [auth.status, auth.epoch, auth.expiresAt]);
  return children;
}
