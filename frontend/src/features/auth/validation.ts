import { z } from "zod";
export const loginSchema = z.object({
  username: z.string().trim().min(1, "Nhập tên đăng nhập."),
  password: z.string().min(1, "Nhập mật khẩu."),
});
export const emailSchema = z.object({
  email: z
    .string()
    .trim()
    .email("Nhập địa chỉ email hợp lệ.")
    .max(320, "Email quá dài."),
});
export const codeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Mã xác minh gồm 6 chữ số."),
});
const password = z
  .string()
  .min(8, "Mật khẩu cần ít nhất 8 ký tự.")
  .max(72, "Mật khẩu tối đa 72 ký tự.")
  .refine(
    (v) => new TextEncoder().encode(v).length <= 72,
    "Mật khẩu tối đa 72 byte UTF-8.",
  );
export const resetSchema = z
  .object({ newPassword: password, confirmPassword: password })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ["confirmPassword"],
    message: "Mật khẩu xác nhận chưa khớp.",
  });
