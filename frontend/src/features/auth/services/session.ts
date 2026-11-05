import type { User } from "../types";
type State = {
  status: "restoring" | "anonymous" | "authenticated";
  user: User | null;
  token: string | null;
  expiresAt: number;
  reason: string | null;
  epoch: number;
};
let state: State = {
  status: "restoring",
  user: null,
  token: null,
  expiresAt: 0,
  reason: null,
  epoch: 0,
};
const listeners = new Set<() => void>();
function update(next: State) {
  state = next;
  listeners.forEach((fn) => fn());
}
export const session = {
  get: () => state,
  subscribe: (fn: () => void) => {
    listeners.add(fn);
    return () => {
      listeners.delete(fn);
    };
  },
  bootstrap: () => {
    if (state.status === "restoring") update({ ...state, status: "anonymous" });
  },
  start: () => {
    const epoch = state.epoch + 1;
    update({
      status: "anonymous",
      user: null,
      token: null,
      expiresAt: 0,
      reason: null,
      epoch,
    });
    return epoch;
  },
  establish: (epoch: number, token: string, user: User, expiresIn: number) => {
    if (state.epoch !== epoch) return false;
    update({
      epoch,
      status: "authenticated",
      token,
      user,
      expiresAt: Date.now() + expiresIn * 1000,
      reason: null,
    });
    return true;
  },
  clear: (reason: string | null = null) =>
    update({
      epoch: state.epoch + 1,
      status: "anonymous",
      user: null,
      token: null,
      expiresAt: 0,
      reason,
    }),
  expire: (epoch: number) => {
    if (state.epoch === epoch && state.token)
      session.clear("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  },
};
