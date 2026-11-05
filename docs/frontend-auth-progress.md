# Frontend authentication foundation

Workspace xác minh: D:/FULearning/Fall26/PRM392/project/myfschoolse1947, nhánh admin-school. Thay đổi có sẵn `docs/API_INVENTORY.md` được giữ nguyên. Không tìm thấy AGENTS.md trong workspace. Thư mục frontend trống khi bắt đầu; đã tạo React/Vite/TypeScript ở đó, không sửa Flutter.

## Đã triển khai

- Router/provider, layout auth/app, hệ thống CSS responsive và shared UI có label/focus/error states.
- Login username + password + source WEB, gọi /me rồi thiết lập phiên và điều hướng theo role; redirect nội bộ theo quyền.
- Forgot email với thông báo trung tính, gửi lại/cooldown/Retry-After; OTP 6 số rồi reset password; token memory, không auto-login.
- Memory-only session theo backend thực tế không refresh/logout/revoke. Client public/protected riêng, Bearer, xử lý 401 đúng epoch, không retry mutation hoặc refresh endpoint không tồn tại.
- Logout local, hết phiên theo timer/focus/401, chặn phản hồi cũ khôi phục phiên sau logout.
- Trang đích tối thiểu 4 role với dữ liệu /me, 403/404 và màn hình khởi tạo. Không số liệu giả.
- Backend auth sửa null user khi WEB login, giữ FLUTTER phone; reset kiểm tra UTF-8 BCrypt limit. Không sửa Teacher/Admin nghiệp vụ.
- README frontend đầy đủ API mapping, env/proxy/email, cách chạy, giới hạn và mở rộng theo role.

## Kiểm tra vừa chạy

- `npm run typecheck`: đạt.
- `npm run lint`: đạt.
- `npm test`: 20/20 đạt (mock chỉ trong test).
- `npm run build`: đạt, output frontend/dist; cảnh báo annotation thư viện Zod.
- `mvn -f backend/pom.xml -Dtest=AuthServiceWebTest test`: 6/6 đạt.
- `mvn -f backend/pom.xml clean verify`: 44/44 đạt, build JAR thành công. Log cục bộ backend/frontend-auth-verification.log, fresh Surefire reports.
- HTTP GET http://127.0.0.1:5173/login: 200 với Vite entry.
- git diff --check: đạt.

## Giới hạn còn lại

Không hỗ trợ giữ đăng nhập qua reload, refresh single-flight/rotation hoặc server revoke vì backend không có hợp đồng đó. Đã chọn phương án memory được user cho phép, không giả báo hỗ trợ phiên bền vững. JWT cũ chưa hết hạn không bị server thu hồi khi logout local.

Không gửi email thật. Chưa chạy browser UI desktop/mobile và frontend-backend thật: browser host unavailable, Chrome native bị công cụ dừng do không xác định URL đủ tin cậy. Đã dừng mọi thao tác UI theo thông báo công cụ; tiếp tục hoàn thành code/test độc lập.

Bước tiếp theo khi browser khả dụng: mở dev server ở desktop/mobile; kiểm tra tab/focus/overflow, hiển thị lỗi, reset qua SMTP sandbox và login với tài khoản disposable trong MySQL test. Nếu cần persistence, phải bổ sung thiết kế refresh cookie HttpOnly/rotation/revoke phía backend trước khi bật khôi phục phiên. Không còn phần code nền tảng độc lập bị bỏ dở.
