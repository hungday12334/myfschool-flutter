import {
  useState,
  type InputHTMLAttributes,
  type ButtonHTMLAttributes,
  type ReactNode,
} from "react";
import { Eye, EyeOff, LoaderCircle, GraduationCap } from "lucide-react";
export function Brand() {
  return (
    <a className="brand" href="/">
      <span className="brand-icon">
        <GraduationCap size={25} />
      </span>
      <span>
        myf<span className="brand-light">school</span>
        <small>KẾT NỐI ĐỂ TRƯỞNG THÀNH</small>
      </span>
    </a>
  );
}
export function Button({
  children,
  loading = false,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean }) {
  return (
    <button
      {...props}
      className={`button ${props.className || ""}`}
      disabled={loading || props.disabled}
      aria-busy={loading}
    >
      {loading && <LoaderCircle className="spin" size={18} />} {children}
    </button>
  );
}
export function Input({
  label,
  error,
  id,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  id: string;
}) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        {...props}
        id={id}
        aria-invalid={!!error}
        aria-describedby={error ? `${id}-error` : undefined}
      />
      {error && (
        <span className="field-error" id={`${id}-error`}>
          {error}
        </span>
      )}
    </div>
  );
}
export function PasswordInput({
  label,
  error,
  id,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  id: string;
}) {
  const [show, setShow] = useState(false);
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="password-wrap">
        <input
          {...props}
          id={id}
          type={show ? "text" : "password"}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setShow(!show)}
          aria-label={show ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          aria-pressed={show}
        >
          {show ? <EyeOff size={19} /> : <Eye size={19} />}
        </button>
      </div>
      {error && (
        <span className="field-error" id={`${id}-error`}>
          {error}
        </span>
      )}
    </div>
  );
}
export function Alert({
  children,
  tone = "error",
}: {
  children: ReactNode;
  tone?: "error" | "success" | "info";
}) {
  return (
    <div
      className={`alert ${tone}`}
      role={tone === "error" ? "alert" : "status"}
    >
      {children}
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading-screen" role="status">
      <GraduationCap size={42} />
      <LoaderCircle className="spin" />
      <p>Đang chuẩn bị không gian của bạn…</p>
    </div>
  );
}
