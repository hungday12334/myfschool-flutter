import { TeacherDashboard } from "../features/teacher/TeacherDashboard";
import { Link } from "react-router-dom";
import { ArrowRight, ShieldCheck, BookOpen } from "lucide-react";
import { useAuth } from "../features/auth/hooks/useAuth";
import { home, roleNames } from "../features/auth/roles";
import { Alert } from "../shared/components/ui";
import { signOut } from "../features/auth/services/auth";
export function RoleHome({ role }: { role: string }) { return role === "TEACHER" ? <TeacherDashboard /> : <DefaultRoleHome role={role} />; }
function DefaultRoleHome({ role }: { role: string }) {
  const { user } = useAuth();
  return (
    <>
      <div className="workspace-kicker">
        MYFSCHOOL / {roleNames[role]?.toUpperCase()}
      </div>
      <section className="welcome">
        <span className="eyebrow">KHÔNG GIAN CỦA BẠN</span>
        <h1>Xin chào, {user?.username}.</h1>
        <p>Chào mừng bạn trở lại với MyFSchool.</p>
        <span className="role-badge">
          <ShieldCheck size={16} />
          {roleNames[role]}
        </span>
      </section>
      {user?.mustChangePassword && (
        <Alert tone="info">
          Tài khoản cần cập nhật mật khẩu. Sử dụng luồng quên mật khẩu với email
          đã đăng ký hoặc liên hệ quản trị viên nhà trường.
        </Alert>
      )}
      <div className="home-grid">
        <section className="info-card">
          <h2>Thông tin tài khoản</h2>
          <dl>
            <div>
              <dt>Tên đăng nhập</dt>
              <dd>{user?.username}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{user?.email || "Chưa cập nhật"}</dd>
            </div>
            <div>
              <dt>Số điện thoại</dt>
              <dd>{user?.phone || "Chưa cập nhật"}</dd>
            </div>
            <div>
              <dt>Vai trò</dt>
              <dd>{user?.roles.map((r) => roleNames[r] || r).join(", ")}</dd>
            </div>
          </dl>
        </section>
        <section className="info-card next-card">
          <BookOpen size={34} />
          <h2>Sẵn sàng cho bước tiếp theo</h2>
          <p>
            Đây là trang khởi đầu dành cho {roleNames[role]?.toLowerCase()}. Các
            chức năng nghiệp vụ sẽ được bổ sung trong giai đoạn tiếp theo.
          </p>
          <span className="small-label">
            THÔNG TIN TÀI KHOẢN ĐƯỢC LẤY TỪ HỆ THỐNG
          </span>
        </section>
      </div>
    </>
  );
}
export function StatusPage({ code }: { code: "403" | "404" }) {
  const { user } = useAuth();
  return (
    <main className="status-page">
      <span className="section-number">MYFSCHOOL / {code}</span>
      <h1>
        {code === "403" ? "Không có quyền truy cập" : "Không tìm thấy trang"}
      </h1>
      <p>
        {code === "403"
          ? "Tài khoản của bạn không có quyền mở không gian này."
          : "Đường dẫn có thể đã thay đổi hoặc không còn tồn tại."}
      </p>
      <Link
        className="button"
        to={
          user
            ? home(user.roles) === "/403"
              ? "/login"
              : home(user.roles)
            : "/login"
        }
      >
        Quay về {user ? "trang chính" : "đăng nhập"} <ArrowRight size={17} />
      </Link>
      {user && (
        <button className="text-button" onClick={() => signOut()}>
          Đăng xuất tài khoản hiện tại
        </button>
      )}
    </main>
  );
}
