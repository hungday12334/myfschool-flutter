package com.example.myfschool;

import com.example.myfschool.dto.teacher.TeacherAttendanceDtos.*;
import com.example.myfschool.entity.AttendanceRecord;
import com.example.myfschool.entity.AttendanceSession;
import com.example.myfschool.entity.ClassEnrollment;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.teacher.TeacherAttendanceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TeacherAttendanceIntegrationTest extends TeacherIntegrationSupport {
    @Autowired TeacherAttendanceService attendance;

    private CreateSession subjectSession() {
        return new CreateSession(schoolClass.getId(), assignment.getId(), null, today,
                Shift.MORNING, 1, AttendanceSessionType.SUBJECT);
    }
    private CreateSession homeroomSession() {
        return new CreateSession(schoolClass.getId(), null, null, today,
                Shift.MORNING, null, AttendanceSessionType.HOMEROOM);
    }
    private void denied(Runnable action, ErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(error));
    }

    @Test void createsSessionMarksStudentsAndParentReadsStoredData() throws Exception {
        var created = attendance.create(teacherUser.getId(), subjectSession());
        var marked = attendance.mark(teacherUser.getId(), created.session().id(), new MarkBatch(List.of(
                new Mark(student.getId(), AttendanceStatus.LATE, "Đến muộn"),
                new Mark(secondStudent.getId(), AttendanceStatus.PRESENT, null))));
        assertThat(marked.records()).hasSize(2);
        em.flush(); em.clear();
        assertThat(em.find(AttendanceRecord.class, marked.records().getFirst().id()).getMarkedAt()).isNotNull();
        mvc.perform(get("/api/parent/children/{id}/attendance",student.getId())
                        .header("Authorization",bearer(parentUser,"PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("LATE"));
        mvc.perform(get("/api/parent/children/{id}/attendance/summary",student.getId())
                        .header("Authorization",bearer(parentUser,"PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.late").value(1));
    }

    @Test void duplicateSessionAndMarksConflict() {
        var session=attendance.create(teacherUser.getId(),subjectSession());
        denied(() -> attendance.create(teacherUser.getId(),subjectSession()),ErrorCode.RESOURCE_CONFLICT);
        attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.PRESENT,null))));
        denied(() -> attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.ABSENT,null)))),ErrorCode.RESOURCE_CONFLICT);
        assertThat(attendance.get(teacherUser.getId(),session.session().id()).records()).hasSize(1);
    }

    @Test void crossSchoolAndUnrelatedStudentsCannotBeMarked() {
        var session=attendance.create(teacherUser.getId(),subjectSession());
        denied(() -> attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(
                new Mark(student.getId(),AttendanceStatus.PRESENT,null),new Mark(outsider.getId(),AttendanceStatus.PRESENT,null)))),ErrorCode.ACCESS_DENIED);
        assertThat(attendance.get(teacherUser.getId(),session.session().id()).records()).isEmpty();
        denied(() -> attendance.get(otherUser.getId(),session.session().id()),ErrorCode.ACCESS_DENIED);
        denied(() -> attendance.create(teacherUser.getId(),new CreateSession(unrelatedClass.getId(),null,null,today,Shift.MORNING,null,AttendanceSessionType.HOMEROOM)),ErrorCode.ACCESS_DENIED);
    }

    @Test void subjectTeacherCannotEditAnotherAssignmentsRecordsOrUseHomeroomActions() {
        var session=attendance.create(teacherUser.getId(),subjectSession());
        var marked=attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.PRESENT,null))));
        denied(() -> attendance.update(subjectUser.getId(),marked.records().getFirst().id(),new UpdateRecord(AttendanceStatus.ABSENT,null)),ErrorCode.ACCESS_DENIED);
        denied(() -> attendance.create(subjectUser.getId(),homeroomSession()),ErrorCode.ACCESS_DENIED);
        denied(() -> attendance.homeroomSummary(subjectUser.getId(),schoolClass.getId(),null,null),ErrorCode.ACCESS_DENIED);
    }

    @Test void updatePersistsStatusAndFinalizedSessionRejectsChanges() {
        var session=attendance.create(teacherUser.getId(),subjectSession());
        var marked=attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.PRESENT,null))));
        var changed=attendance.update(teacherUser.getId(),marked.records().getFirst().id(),new UpdateRecord(AttendanceStatus.EXCUSED,"Có phép"));
        assertThat(changed.status()).isEqualTo(AttendanceStatus.EXCUSED);
        attendance.changeStatus(teacherUser.getId(),session.session().id(),new UpdateSessionStatus(AttendanceSessionStatus.COMPLETED));
        em.flush(); em.clear();
        assertThat(em.find(AttendanceSession.class,session.session().id()).getCompletedAt()).isNotNull();
        denied(() -> attendance.update(teacherUser.getId(),changed.id(),new UpdateRecord(AttendanceStatus.PRESENT,null)),ErrorCode.RESOURCE_CONFLICT);
    }

    @Test void summaryDoesNotMultiplyRecordsAndIncludesHomeroomSessions() {
        // A second historical enrollment row must not multiply attendance aggregate joins.
        ClassEnrollment duplicateEnrollment = new ClassEnrollment();
        duplicateEnrollment.setSchoolClass(schoolClass); duplicateEnrollment.setStudent(student);
        duplicateEnrollment.setStatus(EnrollmentStatus.ACTIVE); persist(duplicateEnrollment); em.flush();
        var first=attendance.create(teacherUser.getId(),subjectSession());
        var second=attendance.create(teacherUser.getId(),homeroomSession());
        attendance.mark(teacherUser.getId(),first.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.PRESENT,null),new Mark(secondStudent.getId(),AttendanceStatus.ABSENT,null))));
        attendance.mark(teacherUser.getId(),second.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.EXCUSED,null))));
        var summary=attendance.summary(teacherUser.getId(),schoolClass.getId(),today,today);
        assertThat(summary.totalSessions()).isEqualTo(2);
        assertThat(summary.totalRecords()).isEqualTo(3);
        assertThat(summary.counts()).containsEntry(AttendanceStatus.PRESENT,1L).containsEntry(AttendanceStatus.ABSENT,1L).containsEntry(AttendanceStatus.EXCUSED,1L);
        var home=attendance.homeroomSummary(teacherUser.getId(),null,null,null);
        assertThat(home.students()).hasSize(2);
        assertThat(home.students().stream().mapToLong(StudentSummary::totalRecords).sum()).isEqualTo(3);
        assertThat(attendance.history(teacherUser.getId(),null,null,null,null,null,0,20).totalElements()).isEqualTo(2);
        assertThat(attendance.history(subjectUser.getId(),null,null,null,null,null,0,20).totalElements()).isZero();
    }

    @Test void cancelledSessionsDoNotContributeToSummaries() {
        var session=attendance.create(teacherUser.getId(),subjectSession());
        attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(new Mark(student.getId(),AttendanceStatus.PRESENT,null))));
        attendance.changeStatus(teacherUser.getId(),session.session().id(),new UpdateSessionStatus(AttendanceSessionStatus.CANCELLED));
        assertThat(attendance.summary(teacherUser.getId(),schoolClass.getId(),null,null).totalRecords()).isZero();
    }

    @Test void apiEnforcesRoleAndValidation() throws Exception {
        mvc.perform(get("/api/teacher/attendance")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/teacher/attendance").header("Authorization",bearer(parentUser,"PARENT"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/teacher/attendance/sessions").header("Authorization",bearer(teacherUser,"TEACHER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(get("/api/teacher/attendance").header("Authorization",bearer(teacherUser,"TEACHER"))
                .param("from",today.toString()).param("to",today.minusDays(1).toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/teacher/attendance").header("Authorization",bearer(teacherUser,"TEACHER"))
                .param("size","101")).andExpect(status().isBadRequest());
    }

    @Test void validatesSchemaSessionShapeAndAcademicDates() {
        denied(() -> attendance.create(teacherUser.getId(),new CreateSession(schoolClass.getId(),null,null,today,Shift.MORNING,1,AttendanceSessionType.HOMEROOM)),ErrorCode.INVALID_REQUEST);
        denied(() -> attendance.create(teacherUser.getId(),new CreateSession(schoolClass.getId(),assignment.getId(),null,today,Shift.MORNING,16,AttendanceSessionType.SUBJECT)),ErrorCode.INVALID_REQUEST);
        denied(() -> attendance.create(teacherUser.getId(),new CreateSession(schoolClass.getId(),assignment.getId(),null,year.getStartDate().minusDays(1),Shift.MORNING,1,AttendanceSessionType.SUBJECT)),ErrorCode.INVALID_REQUEST);
    }

    @Test void attendanceUsesMembershipOnSessionDate() {
        jdbc.update("update class_enrollments set enrolled_at=? where student_id=?",
                java.sql.Timestamp.valueOf(today.plusDays(1).atStartOfDay()),student.getId());
        var session=attendance.create(teacherUser.getId(),subjectSession());
        denied(() -> attendance.mark(teacherUser.getId(),session.session().id(),new MarkBatch(List.of(
                new Mark(student.getId(),AttendanceStatus.PRESENT,null)))),ErrorCode.ACCESS_DENIED);
        assertThat(attendance.get(teacherUser.getId(),session.session().id()).records()).isEmpty();
    }
}
