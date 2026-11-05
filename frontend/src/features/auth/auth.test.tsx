import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import MockAdapter from "axios-mock-adapter";
import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { publicHttp, http, errorMessage } from "../../shared/api/http";
import { session } from "./services/session";
import { signIn, signOut } from "./services/auth";
import { Protected } from "./components/RouteGuards";
import { safeReturn } from "./roles";
import { Providers } from "../../app/providers";
import { AppRoutes } from "../../app/router";
import type { User } from "./types";

const user: User = {
  id: 1,
  username: "teacher.test",
  roles: ["TEACHER"],
  email: "teacher@example.org",
  phone: null,
  status: "ACTIVE",
  mustChangePassword: false,
};
const envelope = <T,>(data: T) => ({ code: 200, message: "OK", data });
let publicMock: MockAdapter;
let privateMock: MockAdapter;
beforeEach(() => {
  session.clear();
  publicMock = new MockAdapter(publicHttp);
  privateMock = new MockAdapter(http);
});
afterEach(() => {
  vi.useRealTimers();
  publicMock.restore();
  privateMock.restore();
  session.clear();
});
function mockLogin() {
  publicMock.onPost("/v1/auth/login").reply(
    200,
    envelope({
      accessToken: "memory-token",
      tokenType: "Bearer",
      expiresIn: 3600,
      user,
    }),
  );
  publicMock.onGet("/v1/auth/me").reply(200, envelope(user));
}
function establish() {
  const epoch = session.start();
  session.establish(epoch, "current-token", user, 3600);
}
function app(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Providers>
        <AppRoutes />
      </Providers>
    </MemoryRouter>,
  );
}
describe("Backend-aligned authentication", () => {
  it("expires the in-memory session without a reload or retry", () => {
    vi.useFakeTimers();
    const epoch = session.start();
    session.establish(epoch, "short-token", user, 1);
    app("/teacher");
    expect(
      screen.getByRole("heading", { name: "Xin chào, teacher.test." }),
    ).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(1001));
    expect(
      screen.getByRole("heading", { name: "Đăng nhập" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."),
    ).toBeInTheDocument();
  });
  it("password visibility is keyboard-accessible and does not alter input", async () => {
    const events = userEvent.setup();
    app("/login");
    const password = screen.getByLabelText("Mật khẩu", { exact: true });
    await events.type(password, "private-value");
    await events.click(screen.getByRole("button", { name: "Hiện mật khẩu" }));
    expect(password).toHaveAttribute("type", "text");
    expect(password).toHaveValue("private-value");
    await events.click(screen.getByRole("button", { name: "Ẩn mật khẩu" }));
    expect(password).toHaveAttribute("type", "password");
  });
  it("rate-limited forgot request never claims acceptance", async () => {
    publicMock
      .onPost("/v1/auth/password-reset/request")
      .reply(429, { code: 42900 }, { "Retry-After": "90" });
    const events = userEvent.setup();
    app("/forgot-password");
    await events.type(screen.getByLabelText("Email"), "person@example.org");
    await events.click(screen.getByRole("button", { name: "Gửi mã xác minh" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "quá nhiều yêu cầu",
    );
    expect(
      screen.queryByText("Yêu cầu đã được tiếp nhận."),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Thử lại sau/ })).toBeDisabled();
  });
  it("expired reset token keeps user logged out and offers a new code", async () => {
    publicMock
      .onPost("/v1/auth/password-reset/verify")
      .reply(200, envelope({ resetToken: "expired-token" }));
    publicMock
      .onPost("/v1/auth/password-reset/confirm")
      .reply(400, { code: 40003 });
    const events = userEvent.setup();
    app("/reset-password");
    await events.type(screen.getByLabelText("Email"), "person@example.org");
    await events.type(screen.getByLabelText("Mã xác minh"), "123456");
    await events.click(screen.getByRole("button", { name: "Xác minh mã" }));
    await events.type(
      await screen.findByLabelText("Mật khẩu mới"),
      "Password123",
    );
    await events.type(
      screen.getByLabelText("Xác nhận mật khẩu"),
      "Password123",
    );
    await events.click(
      screen.getByRole("button", { name: "Lưu mật khẩu mới" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "đã hết hạn hoặc đã được sử dụng",
    );
    expect(
      screen.getByRole("link", { name: "Yêu cầu mã mới" }),
    ).toHaveAttribute("href", "/forgot-password");
    expect(session.get().user).toBeNull();
  });
  it("logs in with WEB source then verifies real current user", async () => {
    mockLogin();
    await signIn("teacher.test", "secret");
    expect(JSON.parse(publicMock.history.post[0].data)).toEqual({
      username: "teacher.test",
      password: "secret",
      source: "WEB",
    });
    expect(publicMock.history.get[0].headers?.Authorization).toBe(
      "Bearer memory-token",
    );
    expect(session.get().user).toEqual(user);
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });
  it("rejects invalid login and does not create session", async () => {
    publicMock.onPost("/v1/auth/login").reply(401, { code: 40101 });
    await expect(signIn("wrong", "wrong")).rejects.toBeDefined();
    expect(session.get().token).toBeNull();
  });
  it("does not authenticate if current user lookup fails", async () => {
    mockLogin();
    publicMock.onGet("/v1/auth/me").reply(401, { code: 40102 });
    await expect(signIn("user", "pass")).rejects.toBeDefined();
    expect(session.get().status).toBe("anonymous");
  });
  it("finishes startup without inventing cookie restoration or refresh", () => {
    session.bootstrap();
    expect(session.get().status).toBe("anonymous");
    expect(publicMock.history.post).toHaveLength(0);
  });
  it("attaches bearer and concurrent 401 clears once without retry/unsupported refresh", async () => {
    establish();
    const epoch = session.get().epoch;
    privateMock.onGet("/protected").reply(401);
    await Promise.allSettled([
      http.get("/protected"),
      http.get("/protected"),
      http.get("/protected"),
    ]);
    expect(session.get().epoch).toBe(epoch + 1);
    expect(session.get().token).toBeNull();
    expect(privateMock.history.get).toHaveLength(3);
    expect(publicMock.history.post).toHaveLength(0);
    expect(privateMock.history.get[0].headers?.Authorization).toBe(
      "Bearer current-token",
    );
  });
  it("403 and network failure do not end session or retry", async () => {
    establish();
    privateMock.onGet("/forbidden").reply(403);
    await expect(http.get("/forbidden")).rejects.toBeDefined();
    expect(session.get().user).toEqual(user);
    privateMock.onGet("/network").networkError();
    await expect(http.get("/network")).rejects.toBeDefined();
    expect(session.get().user).toEqual(user);
    expect(privateMock.history.get).toHaveLength(2);
  });
  it("logout during in-flight sign-in cannot restore the old session", async () => {
    let complete!: (value: [number, unknown]) => void;
    publicMock.onPost("/v1/auth/login").reply(
      () =>
        new Promise((resolve) => {
          complete = resolve;
        }),
    );
    publicMock.onGet("/v1/auth/me").reply(200, envelope(user));
    const pending = signIn("teacher.test", "secret");
    await waitFor(() => expect(complete).toBeDefined());
    signOut();
    complete([
      200,
      envelope({
        accessToken: "late-token",
        tokenType: "Bearer",
        expiresIn: 3600,
        user,
      }),
    ]);
    expect(await pending).toBe(false);
    expect(session.get().token).toBeNull();
  });
  it("late old-session 401 cannot clear a new user session", async () => {
    establish();
    let complete!: (value: [number, unknown]) => void;
    privateMock.onGet("/late").reply(
      () =>
        new Promise((resolve) => {
          complete = resolve;
        }),
    );
    const pending = http.get("/late").catch(() => null);
    await waitFor(() => expect(complete).toBeDefined());
    signOut();
    establish();
    complete([401, {}]);
    await pending;
    expect(session.get().token).toBe("current-token");
  });
  it("only permits safe internal redirects for assigned roles", () => {
    expect(safeReturn("/teacher?tab=1", ["TEACHER"])).toBe("/teacher?tab=1");
    for (const path of [
      "//evil.test",
      "/\\evil.test",
      "https://evil.test",
      "/admin-school",
      "/teacher\n",
    ])
      expect(safeReturn(path, ["TEACHER"])).toBe("/teacher");
  });
  it("redirects anonymous protected route to login and wrong role to 403", () => {
    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route element={<Protected role="SCHOOL_ADMIN" />}>
            <Route path="/admin" element={<div>protected</div>} />
          </Route>
          <Route path="/login" element={<div>login destination</div>} />
          <Route path="/403" element={<div>forbidden destination</div>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("login destination")).toBeInTheDocument();
  });
  it("renders 403 for an authenticated user with wrong role", () => {
    establish();
    app("/admin-school");
    expect(
      screen.getByRole("heading", { name: "Không có quyền truy cập" }),
    ).toBeInTheDocument();
  });
  it("login form shows server errors then reaches role home on success", async () => {
    const events = userEvent.setup();
    publicMock.onPost("/v1/auth/login").replyOnce(401, { code: 40101 });
    app("/login");
    await events.type(screen.getByLabelText("Tên đăng nhập"), "teacher.test");
    await events.type(
      screen.getByLabelText("Mật khẩu", { exact: true }),
      "secret",
    );
    await events.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Tên đăng nhập hoặc mật khẩu chưa đúng",
    );
    mockLogin();
    await events.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(
      await screen.findByRole("heading", { name: "Xin chào, teacher.test." }),
    ).toBeInTheDocument();
  });
  it("forgot password sends real request and shows neutral acceptance", async () => {
    publicMock
      .onPost("/v1/auth/password-reset/request")
      .reply(200, envelope(null));
    const events = userEvent.setup();
    app("/forgot-password");
    await events.type(screen.getByLabelText("Email"), "person@example.org");
    await events.click(screen.getByRole("button", { name: "Gửi mã xác minh" }));
    expect(
      await screen.findByText("Yêu cầu đã được tiếp nhận."),
    ).toBeInTheDocument();
    expect(JSON.parse(publicMock.history.post[0].data)).toEqual({
      email: "person@example.org",
    });
    expect(screen.getByRole("button", { name: /Gửi lại sau/ })).toBeDisabled();
  });
  it("OTP exchange and password confirmation do not auto-login or persist token", async () => {
    publicMock
      .onPost("/v1/auth/password-reset/verify")
      .reply(200, envelope({ resetToken: "reset-memory-only" }));
    publicMock
      .onPost("/v1/auth/password-reset/confirm")
      .reply(200, envelope(null));
    const events = userEvent.setup();
    app("/reset-password");
    await events.type(screen.getByLabelText("Email"), "person@example.org");
    await events.type(screen.getByLabelText("Mã xác minh"), "123456");
    await events.click(screen.getByRole("button", { name: "Xác minh mã" }));
    await events.type(
      await screen.findByLabelText("Mật khẩu mới"),
      "StrongPassword123",
    );
    await events.type(
      screen.getByLabelText("Xác nhận mật khẩu"),
      "StrongPassword123",
    );
    await events.click(
      screen.getByRole("button", { name: "Lưu mật khẩu mới" }),
    );
    expect(
      await screen.findByText(
        "Mật khẩu đã được đặt lại. Bạn có thể đăng nhập.",
      ),
    ).toBeInTheDocument();
    expect(JSON.parse(publicMock.history.post[1].data).resetToken).toBe(
      "reset-memory-only",
    );
    expect(session.get().token).toBeNull();
    expect(localStorage.length).toBe(0);
  });
  it("expired OTP is shown without moving to password stage", async () => {
    publicMock
      .onPost("/v1/auth/password-reset/verify")
      .reply(400, { code: 40002 });
    const events = userEvent.setup();
    app("/reset-password");
    await events.type(screen.getByLabelText("Email"), "person@example.org");
    await events.type(screen.getByLabelText("Mã xác minh"), "123456");
    await events.click(screen.getByRole("button", { name: "Xác minh mã" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Mã xác minh không đúng hoặc đã hết hạn",
    );
    expect(screen.queryByLabelText("Mật khẩu mới")).not.toBeInTheDocument();
  });
  it("maps expired reset token and rate limit to safe Vietnamese messages", async () => {
    publicMock
      .onPost("/expired")
      .reply(400, { code: 40003, message: "SQL internal secret" });
    try {
      await publicHttp.post("/expired");
    } catch (e) {
      expect(errorMessage(e)).toContain("đã hết hạn");
      expect(errorMessage(e)).not.toContain("SQL");
    }
  });
});
