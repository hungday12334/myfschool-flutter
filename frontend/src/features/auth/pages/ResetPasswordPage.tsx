import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { z } from "zod";
import { ArrowLeft, KeyRound } from "lucide-react";
import { emailSchema, codeSchema, resetSchema } from "../validation";
import { z as schema } from "zod";
import { authApi } from "../services/auth";
import { errorMessage } from "../../../shared/api/http";
import {
  Input,
  PasswordInput,
  Button,
  Alert,
} from "../../../shared/components/ui";
const verifySchema = emailSchema.extend({ code: codeSchema.shape.code });
export function ResetPasswordPage() {
  const loc = useLocation();
  const navigate = useNavigate();
  const [token, setToken] = useState<string | null>(null);
  const [error, setError] = useState("");
  const verify = useForm<schema.infer<typeof verifySchema>>({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      email: typeof loc.state?.email === "string" ? loc.state.email : "",
    },
  });
  const reset = useForm<z.infer<typeof resetSchema>>({
    resolver: zodResolver(resetSchema),
  });
  return (
    <>
      <Link className="back-link" to="/forgot-password">
        <ArrowLeft size={16} /> Yêu cầu mã mới
      </Link>
      <div className="page-icon">
        <KeyRound />
      </div>
      <span className="section-number">
        {token ? "BƯỚC 2 / 2" : "BƯỚC 1 / 2"}
      </span>
      <h2>{token ? "Đặt mật khẩu mới" : "Xác minh email"}</h2>
      <p className="intro">
        {token
          ? "Chọn mật khẩu mới từ 8 đến 72 ký tự để bảo vệ tài khoản."
          : "Nhập email và mã 6 số bạn nhận được. Mã chỉ được sử dụng một lần."}
      </p>
      {error && <Alert>{error}</Alert>}
      {!token ? (
        <form
          noValidate
          onSubmit={verify.handleSubmit(async (v) => {
            setError("");
            try {
              const result = await authApi.verify(v.email, v.code);
              setToken(result.resetToken);
              verify.reset({ email: v.email, code: "" });
              window.history.replaceState(
                window.history.state,
                "",
                window.location.pathname,
              );
            } catch (e) {
              setError(errorMessage(e));
            }
          })}
        >
          <Input
            id="verify-email"
            label="Email"
            type="email"
            autoComplete="email"
            error={verify.formState.errors.email?.message}
            {...verify.register("email")}
          />
          <Input
            id="code"
            label="Mã xác minh"
            placeholder="000000"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            error={verify.formState.errors.code?.message}
            {...verify.register("code")}
          />
          <Button type="submit" loading={verify.formState.isSubmitting}>
            Xác minh mã
          </Button>
        </form>
      ) : (
        <form
          noValidate
          onSubmit={reset.handleSubmit(async (v) => {
            setError("");
            try {
              await authApi.confirm(token, v.newPassword, v.confirmPassword);
              setToken(null);
              reset.reset();
              navigate("/login", {
                replace: true,
                state: { resetSuccess: true },
              });
            } catch (e) {
              setError(errorMessage(e));
            }
          })}
        >
          <PasswordInput
            id="new-password"
            label="Mật khẩu mới"
            autoComplete="new-password"
            error={reset.formState.errors.newPassword?.message}
            {...reset.register("newPassword")}
          />
          <PasswordInput
            id="confirm-password"
            label="Xác nhận mật khẩu"
            autoComplete="new-password"
            error={reset.formState.errors.confirmPassword?.message}
            {...reset.register("confirmPassword")}
          />
          <Button type="submit" loading={reset.formState.isSubmitting}>
            Lưu mật khẩu mới
          </Button>
        </form>
      )}
    </>
  );
}
