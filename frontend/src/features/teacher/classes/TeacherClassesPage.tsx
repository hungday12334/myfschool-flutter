import { useEffect, useState } from "react";
import {
  Link,
  useNavigate,
  useOutletContext,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  ChevronDown,
  Info,
  LoaderCircle,
  Search,
  UserRound,
  UsersRound,
} from "lucide-react";
import { errorMessage } from "../../../shared/api/http";
import type { Page } from "../attendance/api";
import type { TeacherLayoutContext } from "../layoutContext";
import {
  teacherClassesApi,
  type AssignedClass,
  type ClassStudent,
} from "./api";
import { Pagination } from "./Pagination";
import "./classes.css";

const CLASS_PAGE_SIZE = 4;
const STUDENT_PAGE_SIZE = 6;

function pageNumber(value: string | null) {
  const page = Number(value);
  return Number.isSafeInteger(page) && page >= 0 ? page : 0;
}

function normalize(value: string) {
  return value
    .toLocaleLowerCase("vi")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/đ/g, "d")
    .trim();
}

function listSearch(search: URLSearchParams) {
  const params = new URLSearchParams(search);
  params.delete("student");
  params.delete("studentPage");
  return params.size ? `?${params}` : "";
}

function subjectsOf(schoolClass: AssignedClass, semester = "") {
  return (
    [
      ...new Set(
        schoolClass.assignments
          .filter((a) => !semester || String(a.semester.id) === semester)
          .map((a) => a.subject.name),
      ),
    ].join(", ") || "Chưa phân công môn"
  );
}

function SearchInput({
  value,
  onChange,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}) {
  return (
    <div className="class-search">
      <Search size={18} aria-hidden="true" />
      <input
        type="search"
        value={value}
        maxLength={150}
        aria-label={placeholder}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}

function RoleBadge({
  homeroom,
  detail = false,
}: {
  homeroom: boolean;
  detail?: boolean;
}) {
  return (
    <span className={`class-role${homeroom ? " homeroom" : ""}`}>
      {homeroom ? (detail ? "Lớp chủ nhiệm" : "Chủ nhiệm") : "Bộ môn"}
    </span>
  );
}

function PageState({
  loading,
  message,
  onRetry,
}: {
  loading?: boolean;
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div className="class-page-state" role={onRetry ? "alert" : "status"}>
      {loading ? (
        <LoaderCircle size={28} className="spin" />
      ) : (
        <UsersRound size={30} />
      )}
      <p>{message}</p>
      {onRetry && (
        <button className="class-retry" type="button" onClick={onRetry}>
          Thử lại
        </button>
      )}
    </div>
  );
}

function AssignedClasses({ classes }: { classes: AssignedClass[] }) {
  const [search, setSearch] = useSearchParams();
  const navigate = useNavigate();
  const query = search.get("q") || "";
  const subject = search.get("subject") || "";
  const semester = search.get("semester") || "";
  const role = search.get("role") || "";
  const subjectOptions = [
    ...new Map(
      classes
        .flatMap((c) => c.assignments)
        .map((a) => [a.subject.id, a.subject]),
    ).values(),
  ];
  const semesters = [
    ...new Map(
      classes
        .flatMap((c) => c.assignments)
        .map((a) => [a.semester.id, a.semester]),
    ).values(),
  ];
  const filtered = classes.filter((c) => {
    if (!normalize(c.className).includes(normalize(query))) return false;
    if (role === "homeroom" && !c.homeroom) return false;
    if (role === "subject" && c.homeroom) return false;
    return (
      (!subject && !semester) ||
      c.assignments.some(
        (a) =>
          (!subject || String(a.subject.id) === subject) &&
          (!semester || String(a.semester.id) === semester),
      )
    );
  });
  const page = Math.min(
    pageNumber(search.get("classPage")),
    Math.max(0, Math.ceil(filtered.length / CLASS_PAGE_SIZE) - 1),
  );
  function changeFilter(key: string, value: string) {
    setSearch(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set(key, value);
        else next.delete(key);
        if (key !== "classPage") next.delete("classPage");
        return next;
      },
      { replace: true },
    );
  }
  function classUrl(id: number) {
    return `/teacher/classes/${id}${listSearch(search)}`;
  }

  return (
    <div className="teacher-classes-page">
      <header className="class-page-heading">
        <h1>Lớp học của tôi</h1>
        <p>Danh sách các lớp được phân công giảng dạy và chủ nhiệm.</p>
      </header>
      <div
        className="class-filters"
        role="search"
        aria-label="Lọc lớp được phân công"
      >
        <SearchInput
          value={query}
          onChange={(value) => changeFilter("q", value)}
          placeholder="Tìm tên lớp"
        />
        <div className="class-select">
          <select
            aria-label="Lọc theo môn học"
            value={subject}
            onChange={(e) => changeFilter("subject", e.target.value)}
          >
            <option value="">Tất cả môn học</option>
            {subjectOptions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <ChevronDown size={17} aria-hidden="true" />
        </div>
        <div className="class-select">
          <select
            aria-label="Lọc theo học kỳ"
            value={semester}
            onChange={(e) => changeFilter("semester", e.target.value)}
          >
            <option value="">Tất cả học kỳ</option>
            {semesters.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <ChevronDown size={17} aria-hidden="true" />
        </div>
        <div className="class-select">
          <select
            aria-label="Lọc theo vai trò"
            value={role}
            onChange={(e) => changeFilter("role", e.target.value)}
          >
            <option value="">Tất cả vai trò</option>
            <option value="homeroom">Chủ nhiệm</option>
            <option value="subject">Bộ môn</option>
          </select>
          <ChevronDown size={17} aria-hidden="true" />
        </div>
      </div>
      <p className="class-result-count" aria-live="polite">
        {filtered.length} lớp được phân công
      </p>
      <section
        className="class-list-card"
        aria-label="Danh sách lớp được phân công"
      >
        <div className="class-table-wrap">
          <table className="class-list-table">
            <thead>
              <tr>
                <th scope="col">Lớp học</th>
                <th scope="col">Môn giảng dạy</th>
                <th scope="col">Vai trò</th>
                <th scope="col">Sĩ số</th>
                <th scope="col">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filtered
                .slice(page * CLASS_PAGE_SIZE, (page + 1) * CLASS_PAGE_SIZE)
                .map((c) => {
                  const grade = c.className.match(/^(\d{1,2})/)?.[1];
                  return (
                    <tr
                      key={c.id}
                      className="class-assigned-row"
                      onClick={(event) => {
                        if (!(event.target as HTMLElement).closest("a"))
                          navigate(classUrl(c.id));
                      }}
                    >
                      <td>
                        <div className="class-name-cell">
                          <span className="class-symbol">
                            <UsersRound size={23} />
                          </span>
                          <div>
                            <Link
                              className="class-name-link"
                              to={classUrl(c.id)}
                            >
                              {c.className}
                            </Link>
                            <small>
                              {grade ? `Khối ${grade}` : c.academicYear.name}
                            </small>
                          </div>
                        </div>
                      </td>
                      <td>{subjectsOf(c, semester)}</td>
                      <td>
                        <RoleBadge homeroom={c.homeroom} />
                      </td>
                      <td>{c.studentCount} học sinh</td>
                      <td>
                        <Link
                          className="class-students-link"
                          to={classUrl(c.id)}
                          aria-label={`Xem học sinh lớp ${c.className}`}
                        >
                          Xem học sinh <ArrowRight size={17} />
                        </Link>
                      </td>
                    </tr>
                  );
                })}
            </tbody>
          </table>
          {!filtered.length && (
            <PageState
              message={
                classes.length
                  ? "Không có lớp phù hợp với bộ lọc."
                  : "Bạn chưa được phân công lớp học."
              }
            />
          )}
        </div>
        <Pagination
          page={page}
          pageSize={CLASS_PAGE_SIZE}
          total={filtered.length}
          noun="lớp"
          onChange={(nextPage) => changeFilter("classPage", String(nextPage))}
        />
      </section>
      <p className="class-list-hint">
        <Info size={17} />
        Chọn một lớp để xem danh sách học sinh.
      </p>
    </div>
  );
}

function ClassDetails({ schoolClass }: { schoolClass: AssignedClass }) {
  const [search, setSearch] = useSearchParams();
  const keyword = search.get("student") || "";
  const page = pageNumber(search.get("studentPage"));
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    key: string;
    data?: Page<ClassStudent>;
    error?: string;
  } | null>(null);
  const key = JSON.stringify([schoolClass.id, page, keyword, revision]);
  const roster = result?.key === key ? result.data : undefined;
  const error = result?.key === key ? result.error : undefined;
  const loading = result?.key !== key;
  const semester = search.get("semester") || "";
  const semesterNames = [
    ...new Set(
      schoolClass.assignments
        .filter((a) => !semester || String(a.semester.id) === semester)
        .map((a) => a.semester.name),
    ),
  ].join(", ");

  useEffect(() => {
    const controller = new AbortController();
    // Search the entire class on the server, including students on other pages.
    const timer = setTimeout(
      () => {
        void teacherClassesApi
          .students(
            schoolClass.id,
            {
              page,
              size: STUDENT_PAGE_SIZE,
              keyword: keyword.trim(),
            },
            controller.signal,
          )
          .then((data) => {
            if (controller.signal.aborted) return;
            if (page > 0 && page >= data.totalPages) {
              setSearch(
                (previous) => {
                  const next = new URLSearchParams(previous);
                  next.delete("studentPage");
                  return next;
                },
                { replace: true },
              );
              return;
            }
            setResult({ key, data });
          })
          .catch((e: unknown) => {
            if (!controller.signal.aborted)
              setResult({ key, error: errorMessage(e) });
          });
      },
      keyword ? 250 : 0,
    );
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [schoolClass.id, page, keyword, key, setSearch]);

  function changeStudentSearch(value: string) {
    setSearch(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set("student", value);
        else next.delete("student");
        next.delete("studentPage");
        return next;
      },
      { replace: true },
    );
  }

  return (
    <div className="teacher-classes-page class-detail-page">
      <header className="class-page-heading">
        <Link
          className="class-back-link"
          to={`/teacher/classes${listSearch(search)}`}
        >
          <ArrowLeft size={17} />
          Danh sách lớp
        </Link>
        <div className="class-detail-title">
          <h1>Lớp {schoolClass.className}</h1>
          <RoleBadge homeroom={schoolClass.homeroom} detail />
        </div>
        <p>
          Năm học {schoolClass.academicYear.name}
          {semesterNames && ` · ${semesterNames}`}
        </p>
      </header>
      <section className="class-overview" aria-label="Thông tin lớp học">
        <div>
          <span className="class-symbol">
            <UsersRound size={25} />
          </span>
          <div>
            <span>Sĩ số</span>
            <strong>{schoolClass.studentCount} học sinh</strong>
          </div>
        </div>
        <div>
          <span className="class-symbol">
            <BookOpen size={25} />
          </span>
          <div>
            <span>Môn được phân công</span>
            <strong>{subjectsOf(schoolClass, semester)}</strong>
          </div>
        </div>
        <div>
          <span className="class-symbol">
            <UserRound size={25} />
          </span>
          <div>
            <span>Vai trò</span>
            <strong>
              {schoolClass.homeroom
                ? "Giáo viên chủ nhiệm"
                : "Giáo viên bộ môn"}
            </strong>
          </div>
        </div>
      </section>
      <section
        className="class-students-card"
        aria-labelledby="class-students-heading"
        aria-busy={loading}
      >
        <header className="class-students-heading">
          <div>
            <h2 id="class-students-heading">Danh sách học sinh</h2>
            <p>{schoolClass.studentCount} học sinh trong lớp</p>
          </div>
          <SearchInput
            value={keyword}
            onChange={changeStudentSearch}
            placeholder="Tìm tên hoặc mã học sinh"
          />
        </header>
        <div className="class-table-wrap">
          <table className="class-students-table">
            <thead>
              <tr>
                <th scope="col">STT</th>
                <th scope="col">Mã học sinh</th>
                <th scope="col">Họ và tên</th>
                <th scope="col">Ngày sinh</th>
                <th scope="col">Giới tính</th>
              </tr>
            </thead>
            <tbody>
              {roster?.content.map((student, index) => (
                <tr key={student.id}>
                  <td>{page * STUDENT_PAGE_SIZE + index + 1}</td>
                  <td>{student.studentCode}</td>
                  <td>
                    <div className="class-student-name">
                      <span className="class-student-avatar" aria-hidden="true">
                        {initials(student.fullName)}
                      </span>
                      <strong>{student.fullName}</strong>
                    </div>
                  </td>
                  <td>{formatDate(student.dateOfBirth)}</td>
                  <td>
                    {student.gender
                      ? { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" }[
                          student.gender
                        ]
                      : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && (
            <PageState loading message="Đang tải danh sách học sinh…" />
          )}
          {error && (
            <PageState
              message={error}
              onRetry={() => setRevision((v) => v + 1)}
            />
          )}
          {roster?.content.length === 0 && (
            <PageState
              message={
                keyword
                  ? "Không tìm thấy học sinh phù hợp."
                  : "Lớp chưa có học sinh."
              }
            />
          )}
        </div>
        {roster && (
          <Pagination
            page={page}
            pageSize={STUDENT_PAGE_SIZE}
            total={roster.totalElements}
            noun="học sinh"
            onChange={(nextPage) => {
              setSearch((previous) => {
                const next = new URLSearchParams(previous);
                if (nextPage) next.set("studentPage", String(nextPage));
                else next.delete("studentPage");
                return next;
              });
            }}
          />
        )}
      </section>
    </div>
  );
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/);
  return `${parts[0]?.[0] || ""}${parts.length > 1 ? parts.at(-1)?.[0] || "" : ""}`.toLocaleUpperCase(
    "vi",
  );
}

function formatDate(value: string | null) {
  if (!value) return "—";
  const [year, month, day] = value.split("-");
  return year && month && day ? `${day}/${month}/${year}` : "—";
}

export function TeacherClassesPage() {
  const { classId } = useParams<{ classId: string }>();
  const context = useOutletContext<TeacherLayoutContext | undefined>();
  const setClassName = context?.setClassName;
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    revision: number;
    classes?: AssignedClass[];
    error?: string;
  } | null>(null);
  const loading = result?.revision !== revision;
  const classes = !loading ? result?.classes : undefined;
  const selected = classes?.find((c) => String(c.id) === classId);

  useEffect(() => {
    const controller = new AbortController();
    void teacherClassesApi
      .list(controller.signal)
      .then((classes) => {
        if (!controller.signal.aborted) setResult({ revision, classes });
      })
      .catch((e: unknown) => {
        if (!controller.signal.aborted)
          setResult({ revision, error: errorMessage(e) });
      });
    return () => controller.abort();
  }, [revision]);

  useEffect(() => {
    setClassName?.(selected?.className || null);
    return () => setClassName?.(null);
  }, [selected?.className, setClassName]);

  if (loading || !classes)
    return (
      <div className="teacher-classes-page">
        <header className="class-page-heading">
          <h1>Lớp học của tôi</h1>
          <p>Danh sách các lớp được phân công giảng dạy và chủ nhiệm.</p>
        </header>
        <section className="class-list-card">
          <PageState
            loading={loading}
            message={
              loading
                ? "Đang tải danh sách lớp…"
                : result?.error || "Không thể tải danh sách lớp."
            }
            onRetry={loading ? undefined : () => setRevision((v) => v + 1)}
          />
        </section>
      </div>
    );

  if (classId && !selected)
    return (
      <div className="teacher-classes-page">
        <Link className="class-back-link" to="/teacher/classes">
          <ArrowLeft size={17} />
          Danh sách lớp
        </Link>
        <section className="class-list-card">
          <PageState message="Không tìm thấy lớp trong danh sách được phân công." />
        </section>
      </div>
    );

  return selected ? (
    <ClassDetails key={selected.id} schoolClass={selected} />
  ) : (
    <AssignedClasses classes={classes} />
  );
}
