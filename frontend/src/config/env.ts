const base = import.meta.env.VITE_API_BASE_URL || "/api";
if (!base.startsWith("/") && !/^https?:\/\//.test(base))
  throw new Error("Invalid public API base URL");
export const env = { apiBaseUrl: base.replace(/\/$/, "") };
