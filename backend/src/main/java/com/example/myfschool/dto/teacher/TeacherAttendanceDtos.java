package com.example.myfschool.dto.teacher;

import com.example.myfschool.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Attendance contracts reflect the session and record model; missing marks are never PRESENT. */
public final class TeacherAttendanceDtos {
    private TeacherAttendanceDtos() {}

    public record CreateSession(@NotNull @Positive Long classId,
            @Positive Long teachingAssignmentId, @Positive Long timetableEntryId,
            @NotNull LocalDate sessionDate, @NotNull Shift shift,
            @Min(1) @Max(15) Integer periodNumber, @NotNull AttendanceSessionType sessionType) {}
    public record Mark(@NotNull @Positive Long studentId, @NotNull AttendanceStatus status,
            @Size(max=500) String note) {}
    public record MarkBatch(@NotEmpty @Size(max=200) List<@Valid Mark> records) {}
    public record UpdateRecord(@NotNull AttendanceStatus status, @Size(max=500) String note) {}
    public record UpdateSessionStatus(@NotNull AttendanceSessionStatus status) {}
    public record RecordView(Long id, Long studentId, String studentCode, String studentName,
            AttendanceStatus status, String note, LocalDateTime markedAt) {}
    public record SessionView(Long id, Long classId, String className, Long teachingAssignmentId,
            Long subjectId, String subjectName, Long timetableEntryId, LocalDate sessionDate,
            Shift shift, Integer periodNumber, AttendanceSessionType sessionType,
            AttendanceSessionStatus status, LocalDateTime completedAt, long recordedStudents) {}
    public record SessionDetail(SessionView session, List<RecordView> records) {}
    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record Summary(Long classId, LocalDate from, LocalDate to, long totalSessions,
            long totalRecords, Map<AttendanceStatus, Long> counts) {}
    public record StudentSummary(Long studentId, String studentCode, String studentName,
            long totalRecords, Map<AttendanceStatus, Long> counts) {}
    public record HomeroomSummary(Summary summary, List<StudentSummary> students) {}
}
