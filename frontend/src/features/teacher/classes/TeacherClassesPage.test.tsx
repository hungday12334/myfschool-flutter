import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import MockAdapter from "axios-mock-adapter";
import { MemoryRouter } from "react-router-dom";
import { AppRoutes } from "../../../app/router";
import { Providers } from "../../../app/providers";
import { http } from "../../../shared/api/http";
import { session } from "../../auth/services/session";
import type { AssignedClass, ClassStudent } from "./api";

const year = { id: 1, name: "2026–2027" };
const school = { id: 1, name: "FPT Schools" };
const semester = { id: 1, name: "Học kỳ I" };
const classes: AssignedClass[] = ["10A1", "10A2", "11A1", "11A2", "12A1"].map(
  (className, index) => ({
    id: index + 1,
    className,
    school,
    academicYear: year,
    homeroom: index === 0,
    studentCount: 10,
    assignments: [
      {
        id: index + 1,
        schoolClass: { id: index + 1, name: className },
        subject: { id: 1, name: "Toán học" },
        semester,
        academicYear: year,
      },
      ...(index === 0
        ? [
            {
              id: 6,
              schoolClass: { id: 1, name: className },
              subject: { id: 2, name: "Vật lý" },
              semester: { id: 2, name: "Học kỳ II" },
              academicYear: year,
            },
          ]
        : []),
    ],
  }),
);
const students: ClassStudent[] = Array.from({ length: 10 }, (_, index) => ({
  id: index + 1,
  studentCode: `HS${String(index + 1).padStart(3, "0")}`,
  fullName: index === 0 ? "Nguyễn Hoàng An" : `Học sinh ${index + 1}`,
  dateOfBirth: index === 0 ? "2011-03-12" : null,
  gender: index === 0 ? "MALE" : null,
}));
const envelope = <T,>(data: T) => ({ code: 200, message: "OK", data });
let mock: MockAdapter;

beforeEach(() => {
  mock = new MockAdapter(http);
  mock.onGet("/teacher/classes").reply(200, envelope(classes));
  mock.onGet(/\/teacher\/classes\/\d+\/students/).reply((config) => {
    const { page = 0, size = 6, keyword = "" } = config.params;
    const filtered = students.filter((s) =>
      `${s.fullName} ${s.studentCode}`.includes(keyword),
    );
    return [
      200,
      envelope({
        content: filtered.slice(page * size, (page + 1) * size),
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / size),
      }),
    ];
  });
  const epoch = session.start();
  session.establish(
    epoch,
    "test-token",
    {
      id: 1,
      username: "teacher.test",
      roles: ["TEACHER"],
      email: null,
      phone: null,
      status: "ACTIVE",
      mustChangePassword: false,
    },
    3600,
  );
});

afterEach(() => {
  mock.restore();
  session.clear();
});

function app(path = "/teacher/classes") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Providers>
        <AppRoutes />
      </Providers>
    </MemoryRouter>,
  );
}

describe("Assigned teacher classes and students", () => {
  it("opens classes first, paginates them, and returns from detail to the same class page", async () => {
    const user = userEvent.setup();
    app();
    expect(await screen.findByText("5 lớp được phân công")).toBeInTheDocument();
    expect(
      screen.getAllByRole("link", { name: /Xem học sinh lớp/ }),
    ).toHaveLength(4);
    expect(
      mock.history.get.filter((r) => r.url?.endsWith("/students")),
    ).toHaveLength(0);
    await user.click(screen.getByRole("button", { name: "Trang sau" }));
    expect(
      screen.getByRole("link", { name: "Xem học sinh lớp 12A1" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Xem học sinh lớp 10A1" }),
    ).not.toBeInTheDocument();
    await user.click(
      screen.getByRole("link", { name: "Xem học sinh lớp 12A1" }),
    );
    expect(
      await screen.findByRole("heading", { name: "Lớp 12A1" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Nguyễn Hoàng An")).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Lớp học của tôi" }),
    ).not.toBeInTheDocument();
    const sidebar = screen.getByRole("navigation", {
      name: "Không gian giáo viên",
    });
    expect(within(sidebar).getByRole("link", { name: "Lớp học" })).toHaveClass(
      "active",
    );
    await user.click(screen.getByRole("link", { name: "Danh sách lớp" }));
    expect(
      screen.getByRole("link", { name: "Xem học sinh lớp 12A1" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Danh sách học sinh" }),
    ).not.toBeInTheDocument();
  });

  it("loads six students per page and searches the entire class on the server", async () => {
    const user = userEvent.setup();
    app("/teacher/classes/1");
    expect(await screen.findByText("Nguyễn Hoàng An")).toBeInTheDocument();
    expect(screen.getByText("12/03/2011")).toBeInTheDocument();
    expect(screen.getByText("Nam", { exact: true })).toBeInTheDocument();
    expect(
      screen.getByText("Hiển thị 1–6 trên 10 học sinh"),
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Trang sau" }));
    expect(await screen.findByText("HS007")).toBeInTheDocument();
    expect(screen.queryByText("Nguyễn Hoàng An")).not.toBeInTheDocument();
    expect(
      screen.getByText("Hiển thị 7–10 trên 10 học sinh"),
    ).toBeInTheDocument();
    expect(mock.history.get.at(-1)?.params).toMatchObject({ page: 1, size: 6 });
    await user.type(
      screen.getByRole("searchbox", { name: "Tìm tên hoặc mã học sinh" }),
      "HS010",
    );
    expect(
      await screen.findByText("Hiển thị 1–1 trên 1 học sinh"),
    ).toBeInTheDocument();
    expect(screen.getByText("HS010")).toBeInTheDocument();
    expect(mock.history.get.at(-1)?.params).toMatchObject({
      page: 0,
      size: 6,
      keyword: "HS010",
    });
  });

  it("combines subject and semester filters on the same assignment and filters by role", async () => {
    const user = userEvent.setup();
    app();
    await screen.findByText("5 lớp được phân công");
    await user.selectOptions(
      screen.getByRole("combobox", { name: "Lọc theo vai trò" }),
      "homeroom",
    );
    expect(screen.getByText("1 lớp được phân công")).toBeInTheDocument();
    await user.selectOptions(
      screen.getByRole("combobox", { name: "Lọc theo môn học" }),
      "2",
    );
    await user.selectOptions(
      screen.getByRole("combobox", { name: "Lọc theo học kỳ" }),
      "1",
    );
    expect(
      screen.getByText("Không có lớp phù hợp với bộ lọc."),
    ).toBeInTheDocument();
    await user.selectOptions(
      screen.getByRole("combobox", { name: "Lọc theo học kỳ" }),
      "2",
    );
    expect(
      screen.getByRole("link", { name: "Xem học sinh lớp 10A1" }),
    ).toBeInTheDocument();
  });

  it("does not request students for a class outside the assigned class list", async () => {
    app("/teacher/classes/999");
    expect(
      await screen.findByText(
        "Không tìm thấy lớp trong danh sách được phân công.",
      ),
    ).toBeInTheDocument();
    expect(
      mock.history.get.filter((r) => r.url?.endsWith("/students")),
    ).toHaveLength(0);
  });

  it("handles a failed student request and retries without showing another class list", async () => {
    const user = userEvent.setup();
    mock.resetHandlers();
    mock.onGet("/teacher/classes").reply(200, envelope(classes));
    mock.onGet("/teacher/classes/1/students").replyOnce(500);
    mock.onGet("/teacher/classes/1/students").reply(200, envelope({
      content: students.slice(0, 6), totalElements: 10, totalPages: 2,
    }));
    app("/teacher/classes/1");
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("Nguyễn Hoàng An")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /Xem học sinh lớp/ }),
    ).not.toBeInTheDocument();
  });

  it("shows an empty class list and retries class loading after a network error", async () => {
    const user = userEvent.setup();
    mock.resetHandlers();
    mock.onGet("/teacher/classes").networkErrorOnce();
    mock.onGet("/teacher/classes").reply(200, envelope([]));
    app();
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Thử lại" }));
    await waitFor(() =>
      expect(
        screen.getByText("Bạn chưa được phân công lớp học."),
      ).toBeInTheDocument(),
    );
  });
});
