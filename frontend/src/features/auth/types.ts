export type Role = "SCHOOL_ADMIN" | "TEACHER" | "PARENT" | "STUDENT";
export interface User {
  id: number;
  username: string;
  phone: string | null;
  email: string | null;
  roles: string[];
  status: "ACTIVE" | "LOCKED" | "DISABLED";
  mustChangePassword: boolean;
}
export interface LoginResult {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
export interface Envelope<T> {
  code: number;
  message: string;
  data: T;
}
