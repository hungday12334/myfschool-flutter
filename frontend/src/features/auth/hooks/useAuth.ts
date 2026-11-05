import { useSyncExternalStore } from "react";
import { session } from "../services/session";
export function useAuth() {
  return useSyncExternalStore(session.subscribe, session.get);
}
