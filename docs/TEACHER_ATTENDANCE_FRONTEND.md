# Teacher attendance frontend

Route `/teacher/attendance`, protected by TEACHER, accessible from AppLayout navigation. Uses all eight endpoints in TeacherAttendanceController through the authenticated Axios client. Branch at implementation: frontend.

- Create subject/homeroom sessions from assigned classes; subject requires assignment and period, homeroom is offered only for homeroom classes. Timetable linkage is optional in backend and is omitted by this UI.
- History filters class, subject and date range with server pagination. A session can be opened directly with `?session=ID`.
- Detail loads records and paginated current class roster. Existing historical recorded students are retained even when absent from current roster. Backend validates membership on session date; current roster API has no historical date parameter, so an unrecorded student who has since left cannot be discovered through this roster.
- Explicit status per student: PRESENT, ABSENT, LATE, EXCUSED. Unselected students are not silently marked present. Notes limited to 500 characters. New records POST in batches up to 200; changed existing records PUT individually. These requests are not a single transaction: errors advise reloading to inspect partial saves.
- Complete/cancel require confirmation and do not silently save pending drafts. Closed sessions are read-only, consistent with backend transition rules.
- Class and homeroom summaries use real counts and date filters. Subject history filter does not apply to summary APIs. No invented attendance percentage.
- API errors, loading, empty history and disabled mutation states are included. No mock data in shipped feature.

Production build/typecheck and ESLint passed. Runtime/backend integration and browser visual checks were not performed for this change. Existing orange CSS palette is reused. No backend changes, commits or deployment.
