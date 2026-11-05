import { Outlet } from "react-router-dom";
import { ArrowUpRight, BookOpen, Leaf } from "lucide-react";
import { Brand } from "../shared/components/ui";
export function AuthLayout() {
  return (
    <div className="auth-shell">
      <aside className="auth-story">
        <Brand />
        <div className="story-content">
          <span className="eyebrow">
            <span className="status-dot" /> CỔNG THÔNG TIN TRƯỜNG HỌC
          </span>
          <h1>
            Cùng kết nối.
            <br />
            Cùng <em>lớn lên.</em>
          </h1>
          <p>
            Một không gian chung cho nhà trường, thầy cô và gia đình. Đồng hành
            trong từng bước trưởng thành.
          </p>
          <div className="school-art" aria-hidden="true">
            <div className="art-sun" />
            <div className="art-label">
              <BookOpen size={17} /> Mỗi ngày, một điều mới{" "}
              <ArrowUpRight size={15} />
            </div>
            <div className="art-building">
              <div className="art-roof" />
              <div className="building-title">MYFSCHOOL</div>
              <div className="art-windows">
                {Array.from({ length: 6 }, (_, i) => (
                  <span key={i} />
                ))}
              </div>
              <div className="art-door" />
            </div>
            <div className="art-tree tree-left">
              <Leaf />
            </div>
            <div className="art-tree tree-right">
              <Leaf />
            </div>
            <div className="art-ground" />
          </div>
        </div>
        <footer>
          MỖI KẾT NỐI, MỘT CƠ HỘI PHÁT TRIỂN{" "}
          <span>© {new Date().getFullYear()} MyFSchool</span>
        </footer>
      </aside>
      <main className="auth-main">
        <div className="mobile-brand">
          <Brand />
        </div>
        <div className="auth-card">
          <Outlet />
        </div>
        <footer className="auth-footer">
          Cần hỗ trợ tài khoản? <span>Liên hệ quản trị viên nhà trường.</span>
        </footer>
      </main>
    </div>
  );
}
