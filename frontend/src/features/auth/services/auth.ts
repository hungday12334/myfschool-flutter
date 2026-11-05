import { data, publicHttp } from "../../../shared/api/http";
import type { LoginResult, User } from "../types";
import { session } from "./session";
export const authApi = {
  login: (username: string, password: string) =>
    data<LoginResult>(
      publicHttp.post("/v1/auth/login", { username, password, source: "WEB" }),
    ),
  me: (token: string) =>
    data<User>(
      publicHttp.get("/v1/auth/me", {
        headers: { Authorization: `Bearer ${token}` },
      }),
    ),
  requestReset: (email: string) =>
    data<null>(publicHttp.post("/v1/auth/password-reset/request", { email })),
  verify: (email: string, code: string) =>
    data<{ resetToken: string }>(
      publicHttp.post("/v1/auth/password-reset/verify", { email, code }),
    ),
  confirm: (resetToken: string, newPassword: string, confirmPassword: string) =>
    data<null>(
      publicHttp.post("/v1/auth/password-reset/confirm", {
        resetToken,
        newPassword,
        confirmPassword,
      }),
    ),
};
export async function signIn(username: string, password: string) {
  const epoch = session.start();
  try {
    const result = await authApi.login(username, password);
    const user = await authApi.me(result.accessToken);
    if (
      result.tokenType !== "Bearer" ||
      !result.accessToken ||
      result.expiresIn <= 0
    )
      throw new Error("Invalid session");
    return session.establish(epoch, result.accessToken, user, result.expiresIn);
  } catch (error) {
    if (session.get().epoch === epoch) session.clear();
    throw error;
  }
}
// Backend has no refresh/logout/revoke API. No credentials are persisted by the browser.
export function signOut() {
  session.clear("Bạn đã đăng xuất trên thiết bị này.");
}
