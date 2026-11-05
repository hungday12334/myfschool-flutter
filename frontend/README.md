# MyFSchool Web

Frontend React + Vite + TypeScript trong thư mục `frontend/` riêng, không thay thế ứng dụng Flutter. React Router, Axios, React Hook Form + Zod; giao diện tiếng Việt dùng hệ thống CSS chung, responsive. Không có API mock trong ứng dụng bàn giao; mock chỉ dùng trong test.

## Chạy local

Yêu cầu Node tương thích Vite (máy đã kiểm chứng Node 24.13.1), npm và backend JDK 21/MySQL.

```powershell
cd frontend
npm ci
Copy-Item .env.example .env
npm run dev
```

Mở `http://127.0.0.1:5173`. Vite proxy `/api` đến `http://127.0.0.1:8080`, không cần cookie hoặc thay CORS. Nếu đổi port backend, sửa target trong `vite.config.ts`. Backend:

```powershell
cd backend
mvn spring-boot:run
```

Cấu hình MySQL theo `backend/.env.example` trước khi chạy. Không dùng tài khoản thật cho kiểm thử. Production hosting phải fallback route không phải asset về index.html và reverse proxy `/api` tới backend. `npm run preview` chỉ để xem build local, không phải production server.

`.env.example`: `VITE_API_BASE_URL=/api`. Nếu dùng API khác origin, giá trị phải gồm `/api`, ví dụ `https://api.example.org/api`; backend phải cho phép origin frontend qua CORS_ALLOWED_ORIGIN_PATTERNS. Mọi biến VITE_* đều public trong bundle; không thêm secret vào đây.

## Trang và hợp đồng API đã đọc từ backend

| Frontend | API thực tế | Body / kết quả |
|---|---|---|
| /login | POST /api/v1/auth/login | username, password, source=WEB → accessToken, tokenType, expiresIn, user |
| Sau login | GET /api/v1/auth/me | Bearer token → id, username, phone, email, roles, status, mustChangePassword |
| /forgot-password | POST /api/v1/auth/password-reset/request | email → thông báo trung tính |
| /reset-password, bước 1 | POST /api/v1/auth/password-reset/verify | email, code (6 số) → resetToken |
| /reset-password, bước 2 | POST /api/v1/auth/password-reset/confirm | resetToken, newPassword, confirmPassword → thành công |
| Đăng xuất | Không có endpoint backend | Kết thúc phiên cục bộ |

HTTP layer xử lý envelope `{code,message,data}`, thông báo lỗi tiếng Việt được ánh xạ theo mã backend, không hiển thị message/stack nội bộ. Forgot/reset/login dùng client public riêng: 401 ở các API này không ảnh hưởng phiên khác. Client protected tự gắn Bearer; 401 của đúng phiên kết thúc phiên, 403 không đăng xuất, lỗi mạng không tự retry mutation. Retry-After 429 được xử lý ở gửi lại email; cooldown UI 60 giây sau lần gửi thành công không được coi là backend rate limiter.

Điều hướng role thực tế từ schema:

- SCHOOL_ADMIN → /admin-school
- TEACHER → /teacher
- PARENT → /parent
- STUDENT → /student

User nhiều role có thể chuyển qua các không gian được cấp; ưu tiên mặc định SCHOOL_ADMIN, TEACHER, PARENT, STUDENT. URL yêu cầu trước login chỉ được sử dụng nếu là đường dẫn nội bộ thuộc role hợp lệ. Có trang 403, 404 và trạng thái khởi tạo phiên. Trang đích chỉ hiển thị thông tin thật từ /me, tên role và trạng thái chuẩn bị màn hình nghiệp vụ; không có thống kê giả. Cờ mustChangePassword được thông báo; không tạo một API đổi mật khẩu khi backend chưa có.

## Access token, refresh và reload

Backend hiện **không có refresh token, cookie phiên, endpoint refresh, logout hay revoke**. Login chỉ trả access token Bearer (thời hạn cấu hình JWT_EXPIRATION_SECONDS, mặc định 3600 giây). CORS allowCredentials=false; security stateless, CSRF disabled cho API Bearer hiện tại.

Theo phương án memory được cho phép trong yêu cầu:

- Access token, user và thời điểm hết hạn chỉ nằm trong module memory; không localStorage/sessionStorage/cookie.
- Reload/tab mới cần đăng nhập lại; khởi động chỉ xác định trạng thái local, không giả lập khôi phục phiên.
- Không gọi refresh hay thêm cơ chế single-flight refresh giả khi không có endpoint. Các bài test refresh thành công/thất bại/rotation vì thế không áp dụng; thay bằng test 401 đồng thời chỉ kết thúc phiên một lần, không retry/refresh.
- Logout xóa token/user và tăng epoch. Kết quả login hoặc lỗi 401 cũ không thể khôi phục/xóa nhầm phiên mới. Có timer và kiểm tra khi focus để kết thúc phiên hết hạn.
- Logout không thu hồi JWT trên server; một bản sao JWT đã bị lấy trước logout vẫn có thể dùng đến khi hết hạn. Không tuyên bố đã hỗ trợ revoke hoặc đăng nhập bền vững.
- Không có cache dữ liệu user ngoài auth store; thêm feature sau này phải dọn cache khi epoch thay đổi. Chưa dùng state manager lớn.

Reset token chỉ ở state của trang xác minh, không ghi ra URL/storage/log; rời/reload trang cần xin hoặc xác minh mã mới. Backend mã OTP/reset token tồn tại trong memory server, mỗi loại 10 phút; restart server làm mất chúng và chưa hỗ trợ chia sẻ giữa nhiều instance. Confirm thành công chuyển về login, không tự đăng nhập.

## Email

Backend dùng SmtpEmailService. Cấu hình MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM, MAIL_SMTP_AUTH, MAIL_STARTTLS_ENABLE trong môi trường backend. Local có thể trỏ SMTP sandbox của đội phát triển; frontend không tự giả gửi email. Khi SMTP lỗi, frontend hiện thông báo không thể tiếp nhận, không báo thành công. Không gửi email thật trong test: backend AuthServiceWebTest mock EmailService. Backend chưa có rate limiter reset; UI chỉ xử lý 429 khi server/proxy có trả về.

## Cấu trúc

```text
src/app/                 router, providers, trang nền
src/features/auth/       pages, components, hooks, services, types, roles, validation
src/shared/api/          Axios và error/envelope
src/shared/components/   Input, PasswordInput, Button, Alert, Loading, Brand
src/layouts/             AuthLayout và AppLayout
src/config/              public env
src/test/                setup kiểm thử
```

Thêm màn hình theo role trong app/router, bên dưới Protected và AppLayout. Frontend guard không thay thế quyền backend. School info/dashboard nghiệp vụ không được gọi ở phiên bản nền tảng này.

## Kiểm chứng thực tế

```powershell
npm run typecheck
npm run lint
npm test
npm run build
```

Lần kiểm chứng 2026-11-05:

- Typecheck, ESLint: đạt, không tắt rule để bỏ qua lỗi.
- Vitest: **20 test đạt**, dùng Axios adapter mock và jsdom: login thành công/thất bại, /me lỗi, init không refresh giả, role guard/redirect, token expiry, 401 đồng thời, 403/network không retry, logout trong login pending, phản hồi phiên cũ, password visibility, forgot trung tính/cooldown/429, verify/reset thành công và OTP/reset token không hợp lệ.
- Production build: đạt. Có cảnh báo annotation tree-shaking từ Zod; không ảnh hưởng build.
- Backend `mvn -f backend/pom.xml clean verify`: **44 test đạt**, gồm 6 AuthServiceWebTest mới và 38 test backend hiện có; không failure/error/skip. Sửa tối thiểu backend: WEB login tra username thay vì null; giữ FLUTTER tra phone; kiểm tra giới hạn 72 byte UTF-8 khi reset.
- Dev server trả HTTP 200 tại /login, có entry ứng dụng.
- Chưa xác nhận tích hợp frontend với backend/MySQL/email thật, chưa kiểm tra trực quan desktop/mobile. Browser host không khả dụng; công cụ Chrome native từ chối vì không xác định được URL để áp dụng policy. Không dùng kết quả jsdom làm bằng chứng đã kiểm tra giao diện trên browser.

Không commit, push hay deploy. Tiến độ/tóm tắt được ghi ở `docs/frontend-auth-progress.md`.
