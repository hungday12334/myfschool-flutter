import axios from "axios";
import { env } from "../../config/env";
import { session } from "../../features/auth/services/session";
import type { Envelope } from "../../features/auth/types";
export const publicHttp = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 15000,
  withCredentials: false,
});
export const http = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 15000,
  withCredentials: false,
});
declare module "axios" {
  interface InternalAxiosRequestConfig {
    sessionEpoch?: number;
  }
}
http.interceptors.request.use((config) => {
  const current = session.get();
  config.sessionEpoch = current.epoch;
  if (current.token)
    config.headers.set("Authorization", `Bearer ${current.token}`);
  return config;
});
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.sessionEpoch !== undefined
    )
      session.expire(error.config.sessionEpoch);
    return Promise.reject(error);
  },
);
export async function data<T>(
  request: Promise<{ data: Envelope<T> }>,
): Promise<T> {
  const response = await request;
  if (response.data.code !== 200) throw new Error("API response rejected");
  return response.data.data;
}
export function errorMessage(error: unknown): string {
  if (!axios.isAxiosError(error))
    return "Không thể hoàn tất yêu cầu. Vui lòng thử lại.";
  if (!error.response)
    return "Không thể kết nối máy chủ. Kiểm tra kết nối và thử lại.";
  const code = (error.response.data as Partial<Envelope<unknown>>)?.code;
  const known: Record<number, string> = {
    40101: "Tên đăng nhập hoặc mật khẩu chưa đúng.",
    40301: "Tài khoản đang bị khóa. Vui lòng liên hệ nhà trường.",
    40302: "Tài khoản hoặc trường hiện không hoạt động.",
    40002: "Mã xác minh không đúng hoặc đã hết hạn. Vui lòng yêu cầu mã mới.",
    40003:
      "Phiên đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu mã mới.",
    40004: "Mật khẩu xác nhận chưa khớp.",
    50301: "Chưa thể tiếp nhận yêu cầu lúc này. Vui lòng thử lại sau.",
  };
  if (code && known[code]) return known[code];
  if (error.response.status === 429)
    return "Bạn đã gửi quá nhiều yêu cầu. Vui lòng chờ rồi thử lại.";
  if (error.response.status === 403)
    return "Bạn không có quyền thực hiện thao tác này.";
  if (error.response.status === 401)
    return "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.";
  if (error.response.status === 400)
    return "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.";
  return "Máy chủ chưa thể xử lý yêu cầu. Vui lòng thử lại sau.";
}
export function retryAfter(error: unknown): number {
  if (!axios.isAxiosError(error) || error.response?.status !== 429) return 0;
  const raw = error.response.headers["retry-after"];
  const seconds = Number(raw);
  return Math.min(
    3600,
    Math.max(
      30,
      Number.isFinite(seconds)
        ? seconds
        : Math.ceil((Date.parse(String(raw)) - Date.now()) / 1000) || 60,
    ),
  );
}
