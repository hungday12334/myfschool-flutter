import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { z } from "zod";
import { ArrowRight, LockKeyhole } from "lucide-react";
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
      <span className="section-number">01 / CHÀO MỪNG TRỞ LẠI</span>
      <h2>Đăng nhập</h2>
      <p className="intro">Ngày học mới bắt đầu từ một kết nối.</p>
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
        <Input
          id="username"
          label="Tên đăng nhập"
          placeholder="Tên đăng nhập do nhà trường cấp"
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
          error={errors.username?.message}
          {...register("username")}
        />
        <PasswordInput
          id="password"
          label="Mật khẩu"
          placeholder="Nhập mật khẩu của bạn"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register("password")}
        />
        <div className="form-links">
          <span>
            <LockKeyhole size={13} /> Bảo mật tài khoản
          </span>
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>
        <Button type="submit" loading={isSubmitting}>
          {isSubmitting ? "Đang đăng nhập…" : "Đăng nhập"}{" "}
          {!isSubmitting && <ArrowRight size={18} />}
        </Button>
      </form>
      <div className="form-note">
        Sử dụng tài khoản được nhà trường cung cấp.
        <br />
        Liên hệ quản trị viên nếu bạn chưa có tài khoản.
      </div>
    </>
  );
}
