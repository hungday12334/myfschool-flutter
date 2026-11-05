import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Button, Input } from "../../../shared/components/ui";
import { errorMessage } from "../../../shared/api/http";
import {
  attendanceApi as api,
  labels,
  type Status,
  type ClassInfo,
  type Detail,
  type Student,
  type Page,
  type Session,
  type Summary,
  type HomeSummary,
} from "./api";
const schema = z
  .object({
    classId: z.string().min(1, "Chọn lớp."),
    sessionDate: z.string().min(1, "Chọn ngày."),
    shift: z.string(),
    sessionType: z.string(),
    assignment: z.string(),
    period: z.string(),
  })
  .superRefine((v, c) => {
    if (
      v.sessionType === "SUBJECT" &&
      (!v.assignment ||
        !v.period ||
        Number(v.period) < 1 ||
        Number(v.period) > 15)
    )
      c.addIssue({
        code: "custom",
        path: ["assignment"],
        message: "Chọn phân công và tiết từ 1 đến 15.",
      });
  });
const stateLabel = {
  OPEN: "Đang mở",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
};
export function AttendancePage() {
  const [view, setView] = useState<"session" | "create" | "history" | "summary">("session");
  const [studentQuery, setStudentQuery] = useState("");
  const [chosenSession, setChosenSession] = useState("");
  const [rosterPage, setRosterPage] = useState(0);
  const [summaryPage, setSummaryPage] = useState(0);
  const [search, setSearch] = useSearchParams();
  const id = Number(search.get("session")) || null;
  const [classes, setClasses] = useState<ClassInfo[]>([]),
    [history, setHistory] = useState<Page<Session> | null>(null),
    [detail, setDetail] = useState<Detail | null>(null),
    [students, setStudents] = useState<Student[]>([]);
  const [draft, setDraft] = useState<
    Record<number, { status: Status | ""; note: string }>
  >({});
  const [filter, setFilter] = useState({
      classId: "",
      subjectId: "",
      from: "",
      to: "",
    }),
    [page, setPage] = useState(0),
    [version, setVersion] = useState(0),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null),
    [home, setHome] = useState<HomeSummary | null>(null),
    [confirm, setConfirm] = useState<"COMPLETED" | "CANCELLED" | null>(null);
  const [today] = useState(() => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0,10));
  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: {
      classId: "",
      sessionDate: today,
      shift: "MORNING",
      sessionType: "SUBJECT",
      assignment: "",
      period: "1",
    },
  });
  const selectedClassId = useWatch({ control: form.control, name: "classId" });
  const selected = classes.find((c) => c.id === Number(selectedClassId));
  const type = useWatch({ control: form.control, name: "sessionType" });
  useEffect(() => {
    let live = true;
    api
      .classes()
      .then((v) => {
        if (live) setClasses(v);
      })
      .catch((e) => {
        if (live) setError(errorMessage(e));
      });
    return () => {
      live = false;
    };
  }, []);
  useEffect(() => {
    let live = true;
    queueMicrotask(() => { if(live) setLoading(true); });
    api
      .history({
        ...Object.fromEntries(Object.entries(filter).filter(([, v]) => v)),
        page,
        size: 5,
      })
      .then((v) => {
        if (live) setHistory(v);
      })
      .catch((e) => {
        if (live) setError(errorMessage(e));
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [filter, page, version]);
  useEffect(() => {
    let live = true;
    queueMicrotask(() => { if(live){setDetail(null);setStudents([]);setDraft({});setConfirm(null);setRosterPage(0);} });
    if (!id) return;
    queueMicrotask(() => { if(live) setLoading(true); });
    (async () => {
      try {
        const d = await api.detail(id);
        let list: Student[] = [];
        let p = 0;
        let total = 1;
        while (p < total) {
          const result = await api.roster(d.session.classId, p++);
          list = list.concat(result.content);
          total = result.totalPages;
        }
        if (!live) return;
        setDetail(d);
        setStudents(list);
        const values: typeof draft = {};
        for (const r of d.records)
          values[r.studentId] = { status: r.status, note: r.note || "" };
        setDraft(values);
      } catch (e) {
        if (live) setError(errorMessage(e));
      } finally {
        if (live) setLoading(false);
      }
    })();
    return () => {
      live = false;
    };
  }, [id, version]);
  async function action(fn: () => Promise<unknown>, message: string) {
    if (busy) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await fn();
      setNotice(message);
      setVersion((v) => v + 1);
      setConfirm(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const dates = { from: filter.from || undefined, to: filter.to || undefined };
  const allStudents = [
    ...students,
    ...(detail?.records
      .filter((r) => !students.some((s) => s.id === r.studentId))
      .map((r) => ({
        id: r.studentId,
        studentCode: r.studentCode,
        fullName: r.studentName,
      })) || []),
  ];
  const roster = allStudents.filter(s => `${s.fullName} ${s.studentCode}`.toLocaleLowerCase('vi').includes(studentQuery.toLocaleLowerCase('vi')));
  async function save() {
    if (!detail) return;
    const existing = new Map(detail.records.map((r) => [r.studentId, r]));
    const newRows = allStudents
      .filter((s) => !existing.has(s.id) && draft[s.id]?.status)
      .map((s) => ({
        studentId: s.id,
        status: draft[s.id].status as Status,
        note: draft[s.id].note,
      }));
    await action(async () => {
      for (let i = 0; i < newRows.length; i += 200)
        await api.mark(detail.session.id, newRows.slice(i, i + 200));
      for (const r of detail.records) {
        const d = draft[r.studentId];
        if (d?.status && (d.status !== r.status || d.note !== (r.note || "")))
          await api.update(r.id, d.status, d.note);
      }
    }, "Đã lưu các thay đổi.");
  }
  return (
    <div className="attendance">
      <div className="attendance-top"><div><h1>Điểm danh học sinh</h1><p>Quản lý phiên điểm danh và theo dõi chuyên cần của lớp.</p></div><Button onClick={() => {setSearch({});setView("create");}}>＋ Tạo phiên</Button></div>
      <nav className="attendance-tabs" aria-label="Điểm danh">
        {([['session','Phiên điểm danh'],['history','Lịch sử'],['summary','Thống kê chuyên cần']] as const).map(([tab,label]) => <button key={tab} className={(id ? 'session' : view) === tab ? 'active' : ''} disabled={busy} onClick={() => {setSearch({});setView(tab);setNotice('');}}>{label}</button>)}
      </nav>
      {error && (
        <Alert>
          {error} Nếu thao tác lưu bị gián đoạn, hãy tải lại phiên để kiểm tra
          những bản ghi đã lưu.
        </Alert>
      )}
      {notice && <Alert tone="success">{notice}</Alert>}
      {!id && view === "create" && <section className="info-card">
        <h2>Mở buổi điểm danh</h2>
        <form
          className="attendance-form"
          onSubmit={form.handleSubmit(async (v) => {
            await action(async () => {
              const result = await api.create({
                classId: Number(v.classId),
                sessionDate: v.sessionDate,
                shift: v.shift,
                sessionType: v.sessionType,
                teachingAssignmentId:
                  v.sessionType === "SUBJECT" ? Number(v.assignment) : null,
                periodNumber:
                  v.sessionType === "SUBJECT" ? Number(v.period) : null,
              });
              setSearch({ session: String(result.session.id) });
            }, "Đã mở buổi điểm danh.");
          })}
        >
          <label>
            Lớp
            <select
              {...form.register("classId")}
              onChange={(e) => {
                form.setValue("classId", e.target.value);
                form.setValue("assignment", "");
              }}
            >
              <option value="">Chọn lớp</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.className}
                </option>
              ))}
            </select>
          </label>
          <Input
            id="attendance-date"
            label="Ngày học"
            type="date"
            {...form.register("sessionDate")}
          />
          <label>
            Buổi
            <select {...form.register("shift")}>
              <option value="MORNING">Sáng</option>
              <option value="AFTERNOON">Chiều</option>
              <option value="EVENING">Tối</option>
            </select>
          </label>
          <label>
            Loại
            <select {...form.register("sessionType")}>
              <option value="SUBJECT">Bộ môn</option>
              {selected?.homeroom && (
                <option value="HOMEROOM">Chủ nhiệm</option>
              )}
            </select>
          </label>
          {type === "SUBJECT" && (
            <>
              <label>
                Phân công
                <select {...form.register("assignment")}>
                  <option value="">Chọn môn / học kỳ</option>
                  {selected?.assignments.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.subject.name} · #{a.id}
                    </option>
                  ))}
                </select>
              </label>
              <Input
                id="period"
                label="Tiết"
                type="number"
                min={1}
                max={15}
                {...form.register("period")}
              />
            </>
          )}
          <Button loading={busy} type="submit">
            Mở buổi
          </Button>
        </form>
        {Object.entries(form.formState.errors).map(([k, e]) => (
          <Alert key={k}>{e.message}</Alert>
        ))}
      </section>}
      {!id && (view === "history" || view === "summary") && <section className="info-card">
        <h2>Lịch sử và thống kê</h2>
        <div className="attendance-form">
          <label>
            Lớp
            <select
              value={filter.classId}
              onChange={(e) => {
                setFilter({
                  ...filter,
                  classId: e.target.value,
                  subjectId: "",
                });
                setPage(0);
                setSummary(null);
                setHome(null);
              }}
            >
              <option value="">Tất cả lớp</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.className}
                </option>
              ))}
            </select>
          </label>
          <label>
            Môn
            <select
              value={filter.subjectId}
              onChange={(e) => {
                setFilter({ ...filter, subjectId: e.target.value });
                setPage(0);
              }}
            >
              <option value="">Tất cả môn</option>
              {Array.from(
                new Map(
                  classes
                    .filter(
                      (c) => !filter.classId || c.id === Number(filter.classId),
                    )
                    .flatMap((c) => c.assignments)
                    .map((a) => [a.subject.id, a.subject]),
                ).values(),
              ).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <Input
            id="from"
            label="Từ ngày"
            type="date"
            value={filter.from}
            onChange={(e) => {
              setFilter({ ...filter, from: e.target.value });
              setPage(0);
            }}
          />
          <Input
            id="to"
            label="Đến ngày"
            type="date"
            value={filter.to}
            min={filter.from}
            onChange={(e) => {
              setFilter({ ...filter, to: e.target.value });
              setPage(0);
            }}
          />
          <Button
            disabled={!filter.classId || busy}
            onClick={() =>
              void action(async () => {
                setSummary(await api.summary(Number(filter.classId), dates));
                setView("summary"); setSummaryPage(0);
                setHome(null);
              }, "Đã tải thống kê lớp.")
            }
          >
            Thống kê lớp
          </Button>
          {classes.find((c) => c.id === Number(filter.classId))?.homeroom && (
            <Button
              disabled={busy}
              onClick={() =>
                void action(async () => {
                  const result = await api.homeroom(
                    Number(filter.classId),
                    dates,
                  );
                  setHome(result); setView("summary"); setSummaryPage(0);
                  setSummary(result.summary);
                }, "Đã tải chuyên cần chủ nhiệm.")
              }
            >
              Lớp chủ nhiệm
            </Button>
          )}
        </div>
        {view === "summary" && summary && (
          <div className="attendance-stats">
            <strong>
              {summary.totalSessions} buổi · {summary.totalRecords} lượt đã ghi
            </strong>
            {Object.entries(labels).map(([key, label]) => (
              <span key={key}>
                {label}: {summary.counts[key as Status] || 0}
              </span>
            ))}
            <small>
              Thống kê theo lớp và khoảng ngày, không theo bộ lọc môn. Bản ghi
              chưa điểm danh không được tính là vắng.
            </small>
          </div>
        )}
        {home && <div className="teacher-pager"><button disabled={summaryPage === 0} onClick={() => setSummaryPage(summaryPage - 1)}>←</button><span>Trang {summaryPage + 1} / {Math.max(1, Math.ceil(home.students.length / 5))}</span><button disabled={(summaryPage + 1) * 5 >= home.students.length} onClick={() => setSummaryPage(summaryPage + 1)}>→</button></div>}
        {home && (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Học sinh</th>
                  {Object.values(labels).map((l) => (
                    <th key={l}>{l}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {home.students.slice(summaryPage * 5, summaryPage * 5 + 5).map((s) => (
                  <tr key={s.studentId}>
                    <td>
                      {s.studentCode} · {s.studentName}
                    </td>
                    {Object.keys(labels).map((k) => (
                      <td key={k}>{s.counts[k as Status] || 0}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {view === "history" && <>
        {loading && <p role="status">Đang tải dữ liệu…</p>}
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Lớp / môn</th>
                <th>Buổi / tiết</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {history?.content.map((s) => (
                <tr key={s.id}>
                  <td>{s.sessionDate}</td>
                  <td>
                    {s.className}
                    <br />
                    {s.subjectName || "Chủ nhiệm"}
                  </td>
                  <td>
                    {{ MORNING: "Sáng", AFTERNOON: "Chiều", EVENING: "Tối" }[
                      s.shift
                    ] || s.shift}{" "}
                    / {s.periodNumber || "—"}
                  </td>
                  <td>{stateLabel[s.status]}</td>
                  <td>
                    <button
                      className="text-button"
                      onClick={() => setSearch({ session: String(s.id) })}
                    >
                      Mở phiên #{s.id}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {history?.content.length === 0 && (
          <p>Chưa có buổi điểm danh phù hợp.</p>
        )}
        <div className="attendance-actions">
          <Button
            className="secondary compact"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            Trước
          </Button>
          <span>
            Trang {page + 1} / {Math.max(1, history?.totalPages || 0)}
          </span>
          <Button
            className="secondary compact"
            disabled={!history || page + 1 >= history.totalPages || loading}
            onClick={() => setPage(page + 1)}
          >
            Sau
          </Button>
        </div>
        </>}
      </section>}
      {id && loading && <p role="status">?ang t?i phi?n ?i?m danh?</p>}
      {!id && view === "session" && <section className="info-card attendance-picker"><h2>Phiên điểm danh</h2><div className="attendance-form"><label>Chọn phiên<select value={chosenSession} onChange={e => setChosenSession(e.target.value)}><option value="">Chọn phiên điểm danh</option>{history?.content.map(s => <option key={s.id} value={s.id}>{s.className} · {s.subjectName || "Chủ nhiệm"} · {s.sessionDate}</option>)}</select></label><Button disabled={!chosenSession} onClick={() => setSearch({session:chosenSession})}>Xem phiên</Button><Button className="secondary" onClick={() => {setView("history");}}>Xem tất cả phiên</Button></div><p>Chọn phiên để xem chi tiết hoặc tạo phiên điểm danh mới.</p></section>}
      {detail && (
        <section className="info-card">
          <h2>
            Phiên #{detail.session.id} · {detail.session.className}
          </h2>
          <p>
            {detail.session.sessionDate} ·{" "}
            {detail.session.subjectName || "Chủ nhiệm"} ·{" "}
            {stateLabel[detail.session.status]}
          </p>
          <Button
            className="secondary compact"
            disabled={busy}
            onClick={() => setVersion((v) => v + 1)}
          >
            Tải lại phiên
          </Button>
          <p>
            Chọn trạng thái rõ ràng cho từng học sinh. Học sinh chưa chọn sẽ giữ
            trạng thái chưa điểm danh.
          </p>
          <div className="attendance-session-stats">{[['Sĩ số',allStudents.length],['Có mặt',allStudents.filter(s => draft[s.id]?.status === 'PRESENT').length],['Vắng',allStudents.filter(s => ['ABSENT','EXCUSED'].includes(draft[s.id]?.status || '')).length],['Đi muộn',allStudents.filter(s => draft[s.id]?.status === 'LATE').length]].map(([label,n]) => <div key={label}><span>{label}</span><strong>{n}</strong></div>)}</div>
          <div className="attendance-roster-toolbar"><h2>Danh sách học sinh</h2><span>Đã ghi nhận {allStudents.filter(s => draft[s.id]?.status).length}/{allStudents.length} học sinh</span><input aria-label="Tìm học sinh" placeholder="Tìm tên hoặc mã học sinh" value={studentQuery} onChange={e => {setStudentQuery(e.target.value);setRosterPage(0);}}/><Button className="secondary" disabled={busy || detail.session.status !== 'OPEN'} onClick={() => setDraft(previous => Object.fromEntries(allStudents.map(s => [s.id,{status:'PRESENT' as Status,note:previous[s.id]?.note || ''}])))}>Đánh dấu tất cả có mặt</Button></div>
          {detail && <div className="teacher-pager"><button disabled={rosterPage === 0} onClick={() => setRosterPage(rosterPage - 1)}>←</button><span>Trang {rosterPage + 1} / {Math.max(1, Math.ceil(roster.length / 5))}</span><button disabled={(rosterPage + 1) * 5 >= roster.length} onClick={() => setRosterPage(rosterPage + 1)}>→</button></div>}
          <fieldset disabled={busy || detail.session.status !== "OPEN"}>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Mã học sinh</th>
                    <th>Họ tên</th>
                    <th>Điểm danh</th>
                    <th>Ghi chú</th>
                  </tr>
                </thead>
                <tbody>
                  {roster.slice(Math.min(rosterPage, Math.max(0, Math.ceil(roster.length / 5) - 1)) * 5, (Math.min(rosterPage, Math.max(0, Math.ceil(roster.length / 5) - 1)) + 1) * 5).map((s) => (
                    <tr key={s.id}>
                      <td>{s.studentCode}</td>
                      <td>{s.fullName}</td>
                      <td>
                        <select
                          aria-label={`Trạng thái ${s.fullName}`}
                          className={"attendance-status " + (draft[s.id]?.status || "")}
                          value={draft[s.id]?.status || ""}
                          onChange={(e) =>
                            setDraft({
                              ...draft,
                              [s.id]: {
                                status: e.target.value as Status | "",
                                note: draft[s.id]?.note || "",
                              },
                            })
                          }
                        >
                          <option
                            value=""
                            disabled={detail.records.some(
                              (r) => r.studentId === s.id,
                            )}
                          >
                            Chưa điểm danh
                          </option>
                          {Object.entries(labels).map(([key, label]) => (
                            <option key={key} value={key}>
                              {label}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          aria-label={`Ghi chú ${s.fullName}`}
                          maxLength={500}
                          value={draft[s.id]?.note || ""}
                          onChange={(e) =>
                            setDraft({
                              ...draft,
                              [s.id]: {
                                status: draft[s.id]?.status || "",
                                note: e.target.value,
                              },
                            })
                          }
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="attendance-actions">
              <Button className="secondary" disabled={busy} onClick={() => {const values: typeof draft = {};for(const r of detail.records) values[r.studentId]={status:r.status,note:r.note || ""};setDraft(values);}}>Hủy thay đổi</Button>
              <Button loading={busy} onClick={() => void save()}>
                Lưu điểm danh
              </Button>
              <Button
                className="secondary"
                onClick={() => setConfirm("COMPLETED")}
              >
                Hoàn tất buổi
              </Button>
              <Button
                className="secondary"
                onClick={() => setConfirm("CANCELLED")}
              >
                Hủy buổi
              </Button>
            </div>
          </fieldset>
          {roster.length === 0 && <p>Không có học sinh trong danh sách.</p>}
          {confirm && (
            <div className="attendance-confirm-overlay"><div className="alert info" role="alertdialog" aria-modal="true" aria-label="X?c nh?n tr?ng th?i bu?i">
              <p>
                {confirm === "COMPLETED" ? "Hoàn tất" : "Hủy"} phiên? Thay đổi
                chưa lưu sẽ không được lưu tự động. Sau thao tác này không thể
                chỉnh sửa điểm danh.
              </p>
              <div className="attendance-actions">
                <Button
                  loading={busy}
                  onClick={() =>
                    void action(
                      () => api.status(detail.session.id, confirm),
                      "Đã cập nhật trạng thái buổi.",
                    )
                  }
                >
                  Xác nhận
                </Button>
                <Button
                  className="secondary"
                  disabled={busy}
                  onClick={() => setConfirm(null)}
                >
                  Quay lại
                </Button>
              </div>
            </div></div>
          )}
        </section>
      )}
    </div>
  );
}
