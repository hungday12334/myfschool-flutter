import { data, http } from "../../../shared/api/http";
export type Status = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED";
export const labels: Record<Status, string> = {
  PRESENT: "Có mặt",
  ABSENT: "Vắng",
  LATE: "Đi muộn",
  EXCUSED: "Có phép",
};
export interface Assignment {
  id: number;
  subject: { id: number; name: string };
  schoolClass: { id: number; name: string };
}
export interface ClassInfo {
  id: number;
  className: string;
  homeroom: boolean;
  assignments: Assignment[];
}
export interface Student {
  id: number;
  studentCode: string;
  fullName: string;
}
export interface RecordView {
  id: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  status: Status;
  note: string | null;
}
export interface Session {
  id: number;
  classId: number;
  className: string;
  subjectName: string | null;
  sessionDate: string;
  shift: string;
  periodNumber: number | null;
  sessionType: string;
  status: "OPEN" | "COMPLETED" | "CANCELLED";
  recordedStudents: number;
}
export interface Detail {
  session: Session;
  records: RecordView[];
}
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
}
export interface Summary {
  totalSessions: number;
  totalRecords: number;
  counts: Record<Status, number>;
}
export interface HomeSummary {
  summary: Summary;
  students: {
    studentId: number;
    studentCode: string;
    studentName: string;
    totalRecords: number;
    counts: Record<Status, number>;
  }[];
}
export interface Create {
  classId: number;
  sessionDate: string;
  shift: string;
  sessionType: string;
  teachingAssignmentId: number | null;
  periodNumber: number | null;
}
export const attendanceApi = {
  classes: () => data<ClassInfo[]>(http.get("/teacher/classes")),
  history: (params: object) =>
    data<Page<Session>>(http.get("/teacher/attendance", { params })),
  detail: (id: number) =>
    data<Detail>(http.get(`/teacher/attendance/sessions/${id}`)),
  create: (body: Create) =>
    data<Detail>(http.post("/teacher/attendance/sessions", body)),
  roster: (id: number, page: number) =>
    data<Page<Student>>(
      http.get(`/teacher/classes/${id}/students`, {
        params: { page, size: 100 },
      }),
    ),
  mark: (
    id: number,
    records: { studentId: number; status: Status; note: string }[],
  ) =>
    data<Detail>(
      http.post(`/teacher/attendance/sessions/${id}/records`, { records }),
    ),
  update: (id: number, status: Status, note: string) =>
    data<RecordView>(
      http.put(`/teacher/attendance/records/${id}`, { status, note }),
    ),
  status: (id: number, status: string) =>
    data<Detail>(
      http.put(`/teacher/attendance/sessions/${id}/status`, { status }),
    ),
  summary: (id: number, params: object) =>
    data<Summary>(
      http.get(`/teacher/classes/${id}/attendance/summary`, { params }),
    ),
  homeroom: (id: number, params: object) =>
    data<HomeSummary>(
      http.get("/teacher/homeroom/attendance-summary", {
        params: { ...params, classId: id },
      }),
    ),
};
