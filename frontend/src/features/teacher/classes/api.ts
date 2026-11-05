import { data, http } from "../../../shared/api/http";
import type { Assignment, ClassInfo, Page, Student } from "../attendance/api";

export interface Reference {
  id: number;
  name: string;
}

export interface ClassAssignment extends Assignment {
  semester: Reference;
  academicYear: Reference;
}

export interface AssignedClass extends ClassInfo {
  school: Reference;
  academicYear: Reference;
  studentCount: number;
  assignments: ClassAssignment[];
}

export interface ClassStudent extends Student {
  dateOfBirth: string | null;
  gender: "MALE" | "FEMALE" | "OTHER" | null;
}

export const teacherClassesApi = {
  list: (signal?: AbortSignal) =>
    data<AssignedClass[]>(http.get("/teacher/classes", { signal })),
  students: (
    classId: number,
    params: { page: number; size: number; keyword: string },
    signal?: AbortSignal,
  ) =>
    data<Page<ClassStudent>>(
      http.get(`/teacher/classes/${classId}/students`, { params, signal }),
    ),
};
