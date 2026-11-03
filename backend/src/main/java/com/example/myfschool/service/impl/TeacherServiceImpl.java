package com.example.myfschool.service.impl;

import com.example.myfschool.dto.request.*;
import com.example.myfschool.dto.response.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import com.example.myfschool.repository.*;
import com.example.myfschool.service.TeacherService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherServiceImpl implements TeacherService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final UserRepository users;
    private final TeacherClassAssignmentRepository assignments;
    private final StudentProfileRepository students;
    private final TimetableEntryRepository timetable;
    private final AttendanceRecordRepository attendance;

    public TeacherServiceImpl(UserRepository users, TeacherClassAssignmentRepository assignments,
            StudentProfileRepository students, TimetableEntryRepository timetable,
            AttendanceRecordRepository attendance) {
        this.users = users;
        this.assignments = assignments;
        this.students = students;
        this.timetable = timetable;
        this.attendance = attendance;
    }

    @Override
    public UserResponse getProfile(Long teacherId) {
        User teacher = teacher(teacherId);
        return new UserResponse(teacher.getId(), teacher.getPhoneNumber(), teacher.getFullName(), teacher.getRole());
    }

    @Override
    public List<TeacherScheduleLessonResponse> getSchedule(Long teacherId, LocalDate requestedDate) {
        User teacher = teacher(teacherId);
        LocalDate date = requestedDate == null ? LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) : requestedDate;
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return timetable.findTeacherSchedule(teacher.getFullName(), start, start.plusDays(6)).stream()
                .map(e -> new TeacherScheduleLessonResponse(e.getId(), e.getLessonDate(), e.getPeriodNumber(),
                        e.getSubjectCode(), e.getSubjectName(), e.getRoom(), TIME.format(e.getStartTime()),
                        TIME.format(e.getEndTime())))
                .toList();
    }

    @Override
    public List<TeacherClassResponse> getClasses(Long teacherId) {
        teacher(teacherId);
        return assignments.findByTeacherIdOrderByClassNameAsc(teacherId).stream()
                .map(a -> classResponse(a, students.findByClassNameIgnoreCaseAndSchoolNameIgnoreCaseOrderByFullNameAsc(
                        a.getClassName(), a.getSchoolName()).size()))
                .toList();
    }

    @Override
    public List<TeacherStudentResponse> getStudents(Long teacherId, Long assignmentId, String search) {
        TeacherClassAssignment assignment = ownedAssignment(teacherId, assignmentId);
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return students.findByClassNameIgnoreCaseAndSchoolNameIgnoreCaseOrderByFullNameAsc(
                        assignment.getClassName(), assignment.getSchoolName()).stream()
                .filter(s -> query.isEmpty() || s.getFullName().toLowerCase(Locale.ROOT).contains(query)
                        || s.getStudentCode().toLowerCase(Locale.ROOT).contains(query))
                .map(this::studentResponse).toList();
    }

    @Override
    public TeacherStudentResponse getStudent(Long teacherId, Long assignmentId, Long studentId) {
        TeacherClassAssignment a = ownedAssignment(teacherId, assignmentId);
        StudentProfile s = students.findByIdAndClassNameIgnoreCaseAndSchoolNameIgnoreCase(
                studentId, a.getClassName(), a.getSchoolName())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found in assigned class"));
        return studentResponse(s);
    }

    @Override
    public List<TeacherAttendanceResponse> getAttendance(Long teacherId, Long assignmentId, LocalDate date) {
        TeacherClassAssignment a = ownedAssignment(teacherId, assignmentId);
        List<StudentProfile> roster = roster(a);
        Map<Long, AttendanceRecord> byStudent = attendance.findByStudentIdInAndAttendanceDateOrderByStudentFullNameAsc(
                roster.stream().map(StudentProfile::getId).toList(), date).stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getStudent().getId(), r -> r));
        return roster.stream().map(s -> attendanceResponse(s, byStudent.get(s.getId()), date)).toList();
    }

    @Override
    @Transactional
    public List<TeacherAttendanceResponse> saveAttendance(Long teacherId, Long assignmentId, LocalDate date,
            List<AttendanceMarkRequest> marks) {
        TeacherClassAssignment a = ownedAssignment(teacherId, assignmentId);
        if (marks == null || marks.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Attendance marks cannot be empty");
        }
        Set<Long> seen = new HashSet<>();
        for (AttendanceMarkRequest mark : marks) {
            if (!seen.add(mark.studentId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Duplicate student in attendance request");
            }
            StudentProfile student = students.findByIdAndClassNameIgnoreCaseAndSchoolNameIgnoreCase(
                    mark.studentId(), a.getClassName(), a.getSchoolName())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                            "Student is not in a class assigned to this teacher"));
            AttendanceRecord record = attendance.findByStudentIdAndAttendanceDate(student.getId(), date)
                    .orElseGet(() -> AttendanceRecord.builder().student(student).attendanceDate(date)
                            .status(mark.status()).note(mark.note()).build());
            if (record.getId() == null) attendance.save(record);
            else {
                record.update(mark.status(), mark.note());
                attendance.save(record);
            }
        }
        return getAttendance(teacherId, assignmentId, date);
    }

    @Override
    @Transactional
    public TeacherClassResponse assignClass(CreateTeacherClassAssignmentRequest request) {
        User teacher = users.findById(request.teacherId())
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found"));
        String className = request.className().trim();
        String schoolName = request.schoolName().trim();
        if (!students.existsByClassNameIgnoreCaseAndSchoolNameIgnoreCase(className, schoolName)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Class has no student profiles");
        }
        if (assignments.existsByTeacherIdAndClassNameIgnoreCaseAndSchoolNameIgnoreCase(
                teacher.getId(), className, schoolName)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Teacher is already assigned to this class");
        }
        TeacherClassAssignment a = assignments.save(TeacherClassAssignment.builder().teacher(teacher)
                .className(className).schoolName(schoolName).build());
        return classResponse(a, students.findByClassNameIgnoreCaseAndSchoolNameIgnoreCaseOrderByFullNameAsc(
                className, schoolName).size());
    }

    private User teacher(Long id) {
        return users.findById(id).filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found"));
    }

    private TeacherClassAssignment ownedAssignment(Long teacherId, Long assignmentId) {
        return assignments.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Assigned class not found"));
    }

    private List<StudentProfile> roster(TeacherClassAssignment a) {
        return students.findByClassNameIgnoreCaseAndSchoolNameIgnoreCaseOrderByFullNameAsc(
                a.getClassName(), a.getSchoolName());
    }

    private TeacherClassResponse classResponse(TeacherClassAssignment a, int count) {
        return new TeacherClassResponse(a.getId(), a.getClassName(), a.getSchoolName(), count);
    }

    private TeacherStudentResponse studentResponse(StudentProfile s) {
        return new TeacherStudentResponse(s.getId(), s.getStudentCode(), s.getFullName(), s.getClassName(),
                s.getSchoolName(), null);
    }

    private TeacherAttendanceResponse attendanceResponse(StudentProfile s, AttendanceRecord record, LocalDate date) {
        return new TeacherAttendanceResponse(s.getId(), s.getStudentCode(), s.getFullName(),
                record == null ? null : record.getStatus().name(), record == null ? null : record.getNote(), date);
    }
}
