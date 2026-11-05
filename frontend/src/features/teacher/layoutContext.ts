import type { Dispatch, SetStateAction } from "react";

export interface TeacherLayoutContext {
  setClassName: Dispatch<SetStateAction<string | null>>;
}
