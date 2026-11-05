import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { z } from "zod";
import { ArrowRight, LockKeyhole, UserRound } from "lucide-react";
import { loginSchema } from "../validation";
import { signIn } from "../services/auth";
import { session } from "../services/session";
import { useAuth } from "../hooks/useAuth";
import { safeReturn } from "../roles";
import { errorMessage } from "../../../shared/api/http";
import {
  Input,
  PasswordInput,
  Button,
  Alert,
} from "../../../shared/components/ui";
export function LoginPage() {
  const navigate = useNavigate();
  const loc = useLocation();
  const auth = useAuth();
  const [error, setError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
  });
  return (
    <>
      <h2>Chào mừng trở lại</h2>
      <p className="intro">Đăng nhập để tiếp tục với myFSchool</p>
      {loc.state?.resetSuccess && (
        <Alert tone="success">
          Mật khẩu đã được đặt lại. Bạn có thể đăng nhập.
        </Alert>
      )}
      {auth.reason && <Alert tone="info">{auth.reason}</Alert>}
      {error && <Alert>{error}</Alert>}
      <form
        onSubmit={handleSubmit(async (values) => {
          setError("");
          try {
            if (await signIn(values.username, values.password))
              navigate(
                safeReturn(loc.state?.from, session.get().user?.roles || []),
                { replace: true },
              );
          } catch (e) {
            setError(errorMessage(e));
          }
        })}
        noValidate
      >
        <div className="fpt-input-icon">
          <UserRound aria-hidden="true" />
          <Input
            id="username"
            label="Tên đăng nhập"
            placeholder="Nhập tên đăng nhập"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            error={errors.username?.message}
            {...register("username")}
          />
        </div>
        <div className="fpt-input-icon">
          <LockKeyhole aria-hidden="true" />
          <PasswordInput
            id="password"
            label="Mật khẩu"
            placeholder="Nhập mật khẩu"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register("password")}
          />
        </div>
        <div className="form-links">
          <label
            className="fpt-remember"
            title="Phiên hiện tại chưa hỗ trợ duy trì đăng nhập sau khi tải lại trang."
          >
            <input type="checkbox" disabled />
            Ghi nhớ đăng nhập
          </label>
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>
        <Button type="submit" loading={isSubmitting}>
          {isSubmitting ? "Đang đăng nhập…" : "Đăng nhập"}{" "}
          {!isSubmitting && <ArrowRight size={18} />}
        </Button>
      </form>
      <div className="form-note">Sử dụng tài khoản do nhà trường cung cấp.</div>
    </>
  );
}
