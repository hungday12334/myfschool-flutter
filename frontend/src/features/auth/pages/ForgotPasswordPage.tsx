import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { z } from "zod";
import { ArrowLeft, Mail } from "lucide-react";
import { authApi } from "../services/auth";
import { emailSchema } from "../validation";
import { errorMessage, retryAfter } from "../../../shared/api/http";
import { Input, Button, Alert } from "../../../shared/components/ui";
export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [sent, setSent] = useState(false);
  const [email, setEmail] = useState("");
  const [wait, setWait] = useState(0);
  const pending = useRef(false);
  const [sending, setSending] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof emailSchema>>({
    resolver: zodResolver(emailSchema),
  });
  useEffect(() => {
    if (wait <= 0) return;
    const t = setTimeout(() => setWait(wait - 1), 1000);
    return () => clearTimeout(t);
  }, [wait]);
  const send = useCallback(
    async (value: string) => {
      if (pending.current || wait > 0) return;
      pending.current = true;
      setSending(true);
      setError("");
      try {
        await authApi.requestReset(value);
        setEmail(value);
        setSent(true);
        setWait(60);
      } catch (e) {
        setError(errorMessage(e));
        setWait(retryAfter(e));
      } finally {
        pending.current = false;
        setSending(false);
      }
    },
    [wait],
  );
  return (
    <>
      <Link className="back-link" to="/login">
        <ArrowLeft size={16} /> Quay lại đăng nhập
      </Link>
      <div className="page-icon">
        <Mail />
      </div>
      <h2>{sent ? "Kiểm tra hộp thư" : "Quên mật khẩu?"}</h2>
      <p className="intro">
        {sent
          ? "Nếu email liên kết với tài khoản, bạn sẽ nhận được mã xác minh gồm 6 số. Vui lòng kiểm tra cả thư rác."
          : "Nhập email liên kết với tài khoản. Chúng tôi sẽ tiếp nhận yêu cầu khôi phục mật khẩu."}
      </p>
      {error && <Alert>{error}</Alert>}
      {sent ? (
        <>
          <Alert tone="success">Yêu cầu đã được tiếp nhận.</Alert>
          <Button
            onClick={() => navigate("/reset-password", { state: { email } })}
          >
            Tôi đã có mã xác minh
          </Button>
          <Button
            className="secondary"
            loading={sending}
            disabled={wait > 0}
            onClick={() => void send(email)}
          >
            {wait > 0 ? `Gửi lại sau ${wait}s` : "Gửi lại mã"}
          </Button>
          <button className="text-button" onClick={() => setSent(false)}>
            Dùng email khác
          </button>
        </>
      ) : (
        <form
          noValidate
          onSubmit={(event) => {
            void handleSubmit((v) => send(v.email))(event);
          }}
        >
          <Input
            id="email"
            label="Email"
            type="email"
            placeholder="ban@truong.edu.vn"
            autoComplete="email"
            error={errors.email?.message}
            {...register("email")}
          />
          <Button type="submit" loading={isSubmitting} disabled={wait > 0}>
            {wait > 0 ? `Thử lại sau ${wait}s` : "Gửi mã xác minh"}
          </Button>
        </form>
      )}
      <p className="form-note">Mã xác minh có hiệu lực trong 10 phút.</p>
    </>
  );
}
