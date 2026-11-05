import { Outlet, Link } from "react-router-dom";
export function LoginLayout() {
  return (
    <div className="fpt-login">
      <header className="fpt-login-header">
        <Link to="/login" aria-label="FPT Schools">
          <svg viewBox="45 12 240 120" role="img" aria-label="FPT Schools">
            <defs>
              <clipPath id="fpt-logo-crop">
                <rect x="45" y="12" width="240" height="120" />
              </clipPath>
            </defs>
            <g clipPath="url(#fpt-logo-crop)">
              <image
                href="/images/login-reference.png"
                width="1487"
                height="1058"
              />
            </g>
          </svg>
        </Link>
      </header>
      <main className="fpt-login-main">
        <aside className="fpt-login-story">
          <span className="fpt-tag">CỔNG THÔNG TIN TRƯỜNG HỌC</span>
          <h1>
            Kết nối hôm nay.
            <br />
            Vững bước ngày mai.
          </h1>
          <p>Đồng hành cùng nhà trường, thầy cô và gia đình.</p>
          <svg
            className="fpt-campus"
            viewBox="30 586 780 369"
            aria-hidden="true"
          >
            <defs>
              <clipPath id="fpt-campus-crop">
                <rect x="30" y="586" width="780" height="369" />
              </clipPath>
            </defs>
            <g clipPath="url(#fpt-campus-crop)">
              <image
                href="/images/login-reference.png"
                width="1487"
                height="1058"
              />
            </g>
          </svg>
        </aside>
        <section className="fpt-login-form">
          <Outlet />
        </section>
      </main>
      <footer className="fpt-login-footer">
        <span>© 2026 FPT Schools</span>
        <span>Hỗ trợ đăng nhập</span>
      </footer>
    </div>
  );
}
