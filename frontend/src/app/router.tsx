import { Navigate, Route, Routes } from "react-router-dom";
import { Guest, Protected } from "../features/auth/components/RouteGuards";
import { home, roleHomes } from "../features/auth/roles";
import { useAuth } from "../features/auth/hooks/useAuth";
import { AuthLayout } from "../layouts/AuthLayout";
import { LoginLayout } from "../layouts/LoginLayout";
import { AppLayout } from "../layouts/AppLayout";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { ForgotPasswordPage } from "../features/auth/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "../features/auth/pages/ResetPasswordPage";
import { RoleHome, StatusPage } from "./pages";
import { Loading } from "../shared/components/ui";
import { AttendancePage } from "../features/teacher/attendance/AttendancePage";
import { TeacherClassesPage } from "../features/teacher/classes/TeacherClassesPage";
function Index() {
  const auth = useAuth();
  return auth.status === "restoring" ? (
    <Loading />
  ) : (
    <Navigate to={auth.user ? home(auth.user.roles) : "/login"} replace />
  );
}
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Index />} />
      <Route element={<Guest />}>
        <Route element={<LoginLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        </Route>
        <Route element={<AuthLayout />}>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>
      </Route>
      {Object.entries(roleHomes).map(([role, path]) => (
        <Route key={role} element={<Protected role={role} />}>
          <Route element={<AppLayout />}>
            <Route path={path} element={<RoleHome role={role} />} />
            {role === "TEACHER" && (
              <>
                <Route
                  path="/teacher/attendance"
                  element={<AttendancePage />}
                />
                <Route
                  path="/teacher/classes/:classId?"
                  element={<TeacherClassesPage />}
                />
              </>
            )}
          </Route>
        </Route>
      ))}
      <Route path="/403" element={<StatusPage code="403" />} />
      <Route path="*" element={<StatusPage code="404" />} />
    </Routes>
  );
}
