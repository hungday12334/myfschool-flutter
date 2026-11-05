import { TeacherLayout } from "../features/teacher/TeacherDashboard";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { LogOut, LayoutDashboard, ChevronRight } from "lucide-react";
import { Brand, Button } from "../shared/components/ui";
import { useAuth } from "../features/auth/hooks/useAuth";
import { signOut } from "../features/auth/services/auth";
import { roleHomes, roleNames } from "../features/auth/roles";
export function AppLayout() {
  const auth = useAuth();
  const navigate = useNavigate();
  if (auth.user?.roles.includes("TEACHER")) return <TeacherLayout />;
  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <Brand />
        <span className="nav-label">KHÔNG GIAN LÀM VIỆC</span>
        <nav>
          {auth.user?.roles.includes("TEACHER") && (
            <NavLink to="/teacher/attendance">
              <LayoutDashboard size={18} />
              Điểm danh học sinh
            </NavLink>
          )}
          {auth.user?.roles
            .filter((r) => roleHomes[r])
            .map((r) => (
              <NavLink key={r} to={roleHomes[r]}>
                <LayoutDashboard size={18} />
                {roleNames[r]}
                <ChevronRight size={14} />
              </NavLink>
            ))}
        </nav>
        <div className="sidebar-bottom">
          MyFSchool
          <br />
          <small>Kết nối nhà trường và gia đình</small>
        </div>
      </aside>
      <div className="workspace">
        <header className="app-header">
          <span>Cổng thông tin trường học</span>
          <div>
            <span className="user-chip">
              {auth.user?.username.slice(0, 1).toUpperCase()}
            </span>
            <span>{auth.user?.username}</span>
            <Button
              className="secondary compact"
              onClick={() => {
                signOut();
                navigate("/login", { replace: true });
              }}
            >
              <LogOut size={16} /> Đăng xuất
            </Button>
          </div>
        </header>
        <main className="workspace-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
