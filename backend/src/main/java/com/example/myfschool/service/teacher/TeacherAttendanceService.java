package com.example.myfschool.service.teacher;

import com.example.myfschool.dto.teacher.TeacherAttendanceDtos.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class TeacherAttendanceService {
    private final EntityManager em;
    private final TeacherAccess access;

    @Transactional
    public SessionDetail create(Long userId, CreateSession request) {
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass = access.classAccess(teacher, request.classId());
        // Every creator locks the same class row, including requests for a not-yet-existing slot.
        em.lock(schoolClass, LockModeType.PESSIMISTIC_WRITE);
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) invalid("Lớp học đã lưu trữ");
        if (request.sessionDate() == null || request.shift() == null || request.sessionType() == null)
            invalid("Thiếu ngày, buổi hoặc loại điểm danh");
        if (request.periodNumber() != null && (request.periodNumber() < 1 || request.periodNumber() > 15))
            invalid("Tiết học phải từ 1 đến 15");
        within(request.sessionDate(), schoolClass.getAcademicYear().getStartDate(),
                schoolClass.getAcademicYear().getEndDate());
        TeachingAssignment assignment = null;
        if (request.sessionType() == AttendanceSessionType.SUBJECT) {
            if (request.teachingAssignmentId() == null || request.periodNumber() == null)
                invalid("Điểm danh bộ môn cần phân công và tiết học");
            assignment = access.assignment(teacher, request.teachingAssignmentId());
            if (!assignment.getSchoolClass().getId().equals(schoolClass.getId())) invalid("Phân công không thuộc lớp");
            within(request.sessionDate(), assignment.getSemester().getStartDate(), assignment.getSemester().getEndDate());
        } else {
            access.homeroom(teacher, schoolClass.getId());
            if (request.teachingAssignmentId() != null || request.timetableEntryId() != null || request.periodNumber() != null)
                invalid("Điểm danh chủ nhiệm không gắn phân công bộ môn hoặc tiết học");
        }
        TimetableEntry timetable = null;
        if (request.timetableEntryId() != null) {
            timetable = em.find(TimetableEntry.class, request.timetableEntryId());
            if (timetable == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            if (assignment == null || !timetable.getTeachingAssignment().getId().equals(assignment.getId())
                    || timetable.getDayOfWeek() != request.sessionDate().getDayOfWeek().getValue()
                    || timetable.getShift() != request.shift()
                    || !Objects.equals(timetable.getPeriodNumber(), request.periodNumber()))
                invalid("Thời khóa biểu không khớp phiên điểm danh");
            within(request.sessionDate(), timetable.getEffectiveFrom(), timetable.getEffectiveTo());
        }
        // Use a locking (current) read as MySQL REPEATABLE READ may already have a snapshot
        // from identity/assignment lookup before this transaction acquired the class lock.
        TypedQuery<AttendanceSession> duplicateQuery = em.createQuery("select s from AttendanceSession s where s.schoolClass.id=:classId "
                        + "and s.sessionDate=:date and s.shift=:shift and s.sessionType=:type "
                        + (request.periodNumber() == null ? "and s.periodNumber is null" : "and s.periodNumber=:period"), AttendanceSession.class)
                .setParameter("classId", schoolClass.getId()).setParameter("date", request.sessionDate())
                .setParameter("shift", request.shift()).setParameter("type", request.sessionType())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1);
        if (request.periodNumber() != null) duplicateQuery.setParameter("period", request.periodNumber());
        if (!duplicateQuery.getResultList().isEmpty()) conflict("Phiên điểm danh đã tồn tại; sử dụng phiên hiện có");
        AttendanceSession session = new AttendanceSession();
        session.setSchoolClass(schoolClass);
        session.setTeachingAssignment(assignment);
        session.setTimetableEntry(timetable);
        session.setSessionDate(request.sessionDate());
        session.setShift(request.shift());
        session.setPeriodNumber(request.periodNumber());
        session.setSessionType(request.sessionType());
        session.setStatus(AttendanceSessionStatus.OPEN);
        session.setCreatedBy(teacher.getUser());
        em.persist(session);
        em.flush();
        return new SessionDetail(view(session, 0), List.of());
    }

    @Transactional
    public SessionDetail mark(Long userId, Long sessionId, MarkBatch request) {
        Teacher teacher = access.teacher(userId);
        AttendanceSession session = session(teacher, sessionId, true);
        open(session);
        if (request.records() == null || request.records().isEmpty() || request.records().size() > 200)
            invalid("Cần từ 1 đến 200 bản ghi điểm danh");
        Set<Long> ids = new HashSet<>();
        for (Mark mark : request.records()) {
            if (mark == null || mark.studentId() == null || !ids.add(mark.studentId())) invalid("Học sinh trùng hoặc không hợp lệ");
            validateMark(mark.status(), mark.note());
        }
        boolean existing = !em.createQuery("select r from AttendanceRecord r where r.session.id=:session and r.student.id in :ids", AttendanceRecord.class)
                .setParameter("session", sessionId).setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList().isEmpty();
        if (existing) conflict("Học sinh đã được điểm danh; dùng chức năng sửa bản ghi");
        Map<Long, Student> enrolled = new HashMap<>();
        em.createQuery("select distinct e.student from ClassEnrollment e where e.schoolClass.id=:classId "
                        + "and e.student.id in :ids and e.student.school.id=:school and e.enrolledAt<:end "
                        + "and (e.leftAt is null or e.leftAt>=:start) and (e.status=:active or e.leftAt is not null)", Student.class)
                .setParameter("classId", session.getSchoolClass().getId()).setParameter("ids", ids)
                .setParameter("school", teacher.getSchool().getId())
                .setParameter("end", session.getSessionDate().plusDays(1).atStartOfDay())
                .setParameter("start", session.getSessionDate().atStartOfDay()).setParameter("active", EnrollmentStatus.ACTIVE)
                .getResultList().forEach(student -> enrolled.put(student.getId(), student));
        if (enrolled.size() != ids.size()) throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "Có học sinh không thuộc lớp tại ngày điểm danh");
        for (Mark mark : request.records()) {
            Student student = enrolled.get(mark.studentId());
            AttendanceRecord record = new AttendanceRecord();
            record.setSession(session);
            record.setStudent(student);
            record.setStatus(mark.status());
            record.setNote(mark.note());
            record.setMarkedBy(teacher.getUser());
            record.setMarkedAt(LocalDateTime.now());
            em.persist(record);
        }
        em.flush();
        return detail(session);
    }

    @Transactional
    public RecordView update(Long userId, Long recordId, UpdateRecord request) {
        Teacher teacher = access.teacher(userId);
        AttendanceRecord record = em.find(AttendanceRecord.class, recordId);
        if (record == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AttendanceSession session = session(teacher, record.getSession().getId(), true);
        open(session);
        access.enrolled(teacher, session.getSchoolClass(), record.getStudent().getId(), session.getSessionDate());
        validateMark(request.status(), request.note());
        em.refresh(record);
        record.setStatus(request.status());
        record.setNote(request.note());
        record.setMarkedBy(teacher.getUser());
        record.setMarkedAt(LocalDateTime.now());
        em.flush();
        return view(record);
    }

    @Transactional
    public SessionDetail changeStatus(Long userId, Long sessionId, UpdateSessionStatus request) {
        AttendanceSession session = session(access.teacher(userId), sessionId, true);
        open(session);
        if (request.status() == null || request.status() == AttendanceSessionStatus.OPEN)
            invalid("Chỉ được hoàn tất hoặc hủy phiên đang mở");
        session.setStatus(request.status());
        session.setCompletedAt(request.status() == AttendanceSessionStatus.COMPLETED ? LocalDateTime.now() : null);
        em.flush();
        return detail(session);
    }

    public SessionDetail get(Long userId, Long sessionId) {
        return detail(session(access.teacher(userId), sessionId, false));
    }

    public Page<SessionView> history(Long userId, Long classId, Long subjectId, Long sessionId,
            LocalDate from, LocalDate to, int page, int size) {
        Teacher teacher = access.teacher(userId);
        validateRange(from, to);
        if (page < 0 || size < 1 || size > 100 || (long)page * size > Integer.MAX_VALUE)
            invalid("Phân trang không hợp lệ");
        if (classId != null) access.classAccess(teacher, classId);
        if (sessionId != null) session(teacher, sessionId, false);
        Scope scope = scope(teacher, classId, subjectId, from, to, false);
        if (sessionId != null) { scope.parameters.put("sessionId", sessionId); scope.where += " and s.id=:sessionId"; }
        long count = query("select count(s) from AttendanceSession s" + scope.where, Long.class, scope).getSingleResult();
        List<AttendanceSession> sessions = query("select s from AttendanceSession s join fetch s.schoolClass "
                + "left join fetch s.teachingAssignment a left join fetch a.subject left join fetch s.timetableEntry"
                + scope.where + " order by s.sessionDate desc,s.id desc", AttendanceSession.class, scope)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
        Map<Long, Long> counts = new HashMap<>();
        if (!sessions.isEmpty()) {
            em.createQuery("select r.session.id,count(r) from AttendanceRecord r where r.session.id in :ids and r.student.school.id=:school group by r.session.id", Object[].class)
                    .setParameter("ids", sessions.stream().map(AttendanceSession::getId).toList())
                    .setParameter("school", teacher.getSchool().getId())
                    .getResultList().forEach(row -> counts.put((Long)row[0], (Long)row[1]));
        }
        return new Page<>(sessions.stream().map(s -> view(s, counts.getOrDefault(s.getId(), 0L))).toList(),
                page, size, count, (int)((count + size - 1) / size));
    }

    public Summary summary(Long userId, Long classId, LocalDate from, LocalDate to) {
        Teacher teacher = access.teacher(userId);
        access.classAccess(teacher, classId);
        validateRange(from, to);
        return summary(teacher, classId, from, to);
    }

    public HomeroomSummary homeroomSummary(Long userId, Long classId, LocalDate from, LocalDate to) {
        Teacher teacher = access.teacher(userId);
        if (classId == null) classId = em.createQuery("select c.id from SchoolClass c where c.homeroomTeacher.id=:teacher "
                        + "and c.school.id=:school and c.status=:active order by c.academicYear.startDate desc,c.id", Long.class)
                .setParameter("teacher", teacher.getId()).setParameter("school", teacher.getSchool().getId())
                .setParameter("active", SchoolClassStatus.ACTIVE).setMaxResults(1).getResultStream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        SchoolClass schoolClass = access.homeroom(teacher, classId);
        validateRange(from, to);
        Scope scope = scope(teacher, schoolClass.getId(), null, from, to, true);
        List<Student> students = em.createQuery("select distinct e.student from ClassEnrollment e join e.student st "
                + "where e.schoolClass.id=:classId and st.school.id=:school order by e.student.fullName,e.student.id", Student.class)
                .setParameter("classId", schoolClass.getId()).setParameter("school", teacher.getSchool().getId()).getResultList();
        Map<Long, Map<AttendanceStatus, Long>> counts = new HashMap<>();
        query("select r.student.id,r.status,count(r) from AttendanceRecord r join r.session s" + scope.where
                + " and r.student.school.id=:school group by r.student.id,r.status", Object[].class, scope)
                .getResultList().forEach(row -> counts.computeIfAbsent((Long)row[0], key -> emptyCounts())
                        .put((AttendanceStatus)row[1], (Long)row[2]));
        List<StudentSummary> result = students.stream().map(student -> {
            Map<AttendanceStatus,Long> studentCounts = counts.getOrDefault(student.getId(), emptyCounts());
            return new StudentSummary(student.getId(), student.getStudentCode(), student.getFullName(),
                    studentCounts.values().stream().mapToLong(Long::longValue).sum(), studentCounts);
        }).toList();
        return new HomeroomSummary(summary(teacher, schoolClass.getId(), from, to), result);
    }

    private Summary summary(Teacher teacher, Long classId, LocalDate from, LocalDate to) {
        Scope scope = scope(teacher, classId, null, from, to, true);
        long sessions = query("select count(s) from AttendanceSession s" + scope.where, Long.class, scope).getSingleResult();
        Map<AttendanceStatus,Long> counts = emptyCounts();
        query("select r.status,count(r) from AttendanceRecord r join r.session s" + scope.where
                        + " and r.student.school.id=:school group by r.status", Object[].class, scope)
                .getResultList().forEach(row -> counts.put((AttendanceStatus)row[0], (Long)row[1]));
        return new Summary(classId, from, to, sessions, counts.values().stream().mapToLong(Long::longValue).sum(), counts);
    }

    private AttendanceSession session(Teacher teacher, Long id, boolean write) {
        AttendanceSession session = em.find(AttendanceSession.class, id,
                write ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (session == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.classAccess(teacher, session.getSchoolClass().getId());
        if (session.getSessionType() == AttendanceSessionType.HOMEROOM) {
            access.homeroom(teacher, session.getSchoolClass().getId());
        } else {
            TeachingAssignment assignment = session.getTeachingAssignment();
            if (assignment == null || !assignment.getSchoolClass().getId().equals(session.getSchoolClass().getId()))
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            access.sameSchool(teacher, assignment.getSubject().getSchool().getId());
            if (!assignment.getSemester().getAcademicYear().getId().equals(session.getSchoolClass().getAcademicYear().getId()))
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            boolean homeroom = session.getSchoolClass().getHomeroomTeacher() != null
                    && session.getSchoolClass().getHomeroomTeacher().getId().equals(teacher.getId());
            if (write || !homeroom) access.assignment(teacher, assignment.getId());
        }
        return session;
    }

    private SessionDetail detail(AttendanceSession session) {
        List<RecordView> records = em.createQuery("select r from AttendanceRecord r join fetch r.student st "
                        + "where r.session.id=:session and st.school.id=:school order by st.fullName,st.id", AttendanceRecord.class)
                .setParameter("session", session.getId()).setParameter("school", session.getSchoolClass().getSchool().getId())
                .getResultList().stream().map(this::view).toList();
        return new SessionDetail(view(session, records.size()), records);
    }

    private SessionView view(AttendanceSession s, long count) {
        TeachingAssignment a = s.getTeachingAssignment();
        return new SessionView(s.getId(), s.getSchoolClass().getId(), s.getSchoolClass().getClassName(),
                a == null ? null : a.getId(), a == null ? null : a.getSubject().getId(),
                a == null ? null : a.getSubject().getName(), s.getTimetableEntry() == null ? null : s.getTimetableEntry().getId(),
                s.getSessionDate(), s.getShift(), s.getPeriodNumber(), s.getSessionType(), s.getStatus(), s.getCompletedAt(), count);
    }

    private RecordView view(AttendanceRecord r) {
        return new RecordView(r.getId(), r.getStudent().getId(), r.getStudent().getStudentCode(),
                r.getStudent().getFullName(), r.getStatus(), r.getNote(), r.getMarkedAt());
    }

    private Scope scope(Teacher teacher, Long classId, Long subjectId, LocalDate from, LocalDate to, boolean excludeCancelled) {
        Scope scope = new Scope();
        scope.where = " where s.schoolClass.school.id=:school and (s.schoolClass.homeroomTeacher.id=:teacher "
                + "or exists (select a.id from TeachingAssignment a where a.id=s.teachingAssignment.id "
                + "and a.teacher.id=:teacher and a.status=:active and a.schoolClass.id=s.schoolClass.id "
                + "and a.subject.school.id=:school and a.semester.academicYear.id=s.schoolClass.academicYear.id))";
        scope.parameters.put("school", teacher.getSchool().getId());
        scope.parameters.put("teacher", teacher.getId());
        scope.parameters.put("active", ActiveStatus.ACTIVE);
        if (classId != null) { scope.where += " and s.schoolClass.id=:classId"; scope.parameters.put("classId", classId); }
        if (subjectId != null) { scope.where += " and s.teachingAssignment.subject.id=:subjectId"; scope.parameters.put("subjectId", subjectId); }
        if (from != null) { scope.where += " and s.sessionDate>=:from"; scope.parameters.put("from", from); }
        if (to != null) { scope.where += " and s.sessionDate<=:to"; scope.parameters.put("to", to); }
        if (excludeCancelled) { scope.where += " and s.status<>:cancelled"; scope.parameters.put("cancelled", AttendanceSessionStatus.CANCELLED); }
        return scope;
    }

    private <T> TypedQuery<T> query(String jpql, Class<T> type, Scope scope) {
        TypedQuery<T> query = em.createQuery(jpql, type);
        scope.parameters.forEach(query::setParameter);
        return query;
    }
    private static class Scope { String where; final Map<String,Object> parameters = new HashMap<>(); }
    private Map<AttendanceStatus,Long> emptyCounts() {
        Map<AttendanceStatus,Long> values = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) values.put(status, 0L);
        return values;
    }
    private void validateMark(AttendanceStatus status, String note) {
        if (status == null || note != null && note.length() > 500) invalid("Trạng thái hoặc ghi chú không hợp lệ");
    }
    private void open(AttendanceSession session) {
        if (session.getStatus() != AttendanceSessionStatus.OPEN) conflict("Phiên đã hoàn tất hoặc hủy; không được sửa");
    }
    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) invalid("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
    }
    private void within(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null || from != null && date.isBefore(from) || to != null && date.isAfter(to))
            invalid("Ngày không thuộc thời gian được phép");
    }
    private void invalid(String message) { throw new BusinessException(ErrorCode.INVALID_REQUEST, message); }
    private void conflict(String message) { throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, message); }
}
