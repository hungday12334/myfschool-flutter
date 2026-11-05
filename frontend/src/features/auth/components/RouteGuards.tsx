import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { Loading } from "../../../shared/components/ui";
import { safeReturn } from "../roles";
export function Protected({ role }: { role?: string }) {
  const auth = useAuth();
  const loc = useLocation();
  if (auth.status === "restoring") return <Loading />;
  if (!auth.user)
    return (
      <Navigate
        to="/login"
        state={{ from: loc.pathname + loc.search }}
        replace
      />
    );
  if (role && !auth.user.roles.includes(role))
    return <Navigate to="/403" replace />;
  return <Outlet />;
}
export function Guest() {
  const auth = useAuth();
  const loc = useLocation();
  if (auth.status === "restoring") return <Loading />;
  return auth.user ? (
    <Navigate to={safeReturn(loc.state?.from, auth.user.roles)} replace />
  ) : (
    <Outlet />
  );
}
