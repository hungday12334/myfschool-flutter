package com.example.myfschool;

import static com.example.myfschool.dto.teacher.TeacherGradeDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.service.ParentService;
import com.example.myfschool.service.teacher.TeacherGradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class TeacherGradeIntegrationTest extends TeacherIntegrationSupport {
    @Autowired TeacherGradeService grades;
    @Autowired ParentService parents;
    @Autowired ObjectMapper json;

    private GradeItemResponse item() {
        return grades.createItem(teacherUser.getId(), new CreateGradeItem(assignment.getId(), "Kiểm tra", GradeType.REGULAR,
                BigDecimal.ONE, BigDecimal.TEN, today));
    }

    @Test
    void emptyGradebookAndDefaultSemesterRemainUsable() throws Exception {
        mvc.perform(get("/api/teacher/classes/{classId}/subjects/{subjectId}/grades", schoolClass.getId(), subject.getId())
                        .header("Authorization", bearer(teacherUser, "TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.semester.id").value(semester.getId()))
                .andExpect(jsonPath("$.data.gradeItems").isEmpty()).andExpect(jsonPath("$.data.students.length()").value(2));
    }

    @Test
    void itemConstraintsAndEmptyEvaluationAreValidated() throws Exception {
        mvc.perform(post("/api/teacher/grade-items").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new CreateGradeItem(assignment.getId(), " ", GradeType.REGULAR, BigDecimal.ZERO, BigDecimal.TEN, today))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/teacher/evaluations").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new EvaluationRequest(student.getId(), schoolClass.getId(), semester.getId(), null, null, " "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enteredAndEditedGradesPersistAndAreVisibleToAuthorizedParent() throws Exception {
        GradeItemResponse item = item();
        var saved = grades.enterGrades(teacherUser.getId(), new EnterGrades(item.id(), List.of(
                new GradeEntry(student.getId(), new BigDecimal("8.50"), "Tiến bộ"),
                new GradeEntry(secondStudent.getId(), null, null))));
        em.flush(); em.clear();
        StudentGrade stored = em.find(StudentGrade.class, saved.getFirst().id());
        assertThat(stored.getScore()).isEqualByComparingTo("8.50");
        assertThat(stored.getGradedBy().getId()).isEqualTo(teacherUser.getId());
        assertThat(stored.getGradedAt()).isNotNull();
        mvc.perform(get("/api/parent/children/{id}/subjects/{subjectId}/grades", student.getId(), subject.getId())
                        .param("semesterId", semester.getId().toString()).header("Authorization", bearer(parentUser, "PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].score").value(8.5));
        mvc.perform(put("/api/teacher/grades/{id}", saved.getFirst().id()).header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"score\":9.25,\"comment\":\"Đã kiểm tra\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.score").value(9.25));
        assertThat(parents.subjectGrades(parentUser.getId(), student.getId(), subject.getId(), semester.getId()).getFirst().get("score"))
                .isEqualTo(new BigDecimal("9.25"));
    }

    @Test
    void gradebookMapsStudentsAndPreservesMissingAndZeroScores() throws Exception {
        GradeItemResponse item = item();
        var initial = grades.gradebook(teacherUser.getId(), schoolClass.getId(), subject.getId(), semester.getId());
        assertThat(initial.students()).hasSize(2).allSatisfy(s -> assertThat(s.grades()).isEmpty());
        grades.enterGrades(teacherUser.getId(), new EnterGrades(item.id(), List.of(new GradeEntry(student.getId(), BigDecimal.ZERO, null))));
        mvc.perform(get("/api/teacher/classes/{classId}/subjects/{subjectId}/grades", schoolClass.getId(), subject.getId())
                        .param("semesterId", semester.getId().toString()).header("Authorization", bearer(teacherUser, "TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gradeItems[0].maxScore").value(10))
                .andExpect(jsonPath("$.data.students[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$.data.students[0].grades[0].score").value(0))
                .andExpect(jsonPath("$.data.students[1].grades").isEmpty());
    }

    @Test
    void anotherTeacherCannotCreateOrEditGradesForAnUnassignedAssignment() throws Exception {
        GradeItemResponse item = item();
        var saved = grades.enterGrades(teacherUser.getId(), new EnterGrades(item.id(), List.of(new GradeEntry(student.getId(), BigDecimal.ONE, null))));
        mvc.perform(post("/api/teacher/grade-items").header("Authorization", bearer(subjectUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new CreateGradeItem(assignment.getId(), "X", GradeType.REGULAR, BigDecimal.ONE, BigDecimal.TEN, today))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/teacher/grades/{id}", saved.getFirst().id()).header("Authorization", bearer(subjectUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"score\":7}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/teacher/classes/{classId}/subjects/{subjectId}/grades", schoolClass.getId(), subject.getId())
                        .header("Authorization", bearer(otherUser, "TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidScoresDatesAndDuplicateGradesAreRejected() throws Exception {
        GradeItemResponse item = item();
        for (String score : List.of("-1", "10.01", "1.123")) {
            mvc.perform(post("/api/teacher/grades").header("Authorization", bearer(teacherUser, "TEACHER"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"gradeItemId\":" + item.id() + ",\"grades\":[{\"studentId\":" + student.getId() + ",\"score\":" + score + "}]}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/teacher/grade-items").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new CreateGradeItem(assignment.getId(), "Wrong date", GradeType.REGULAR,
                                BigDecimal.ONE, BigDecimal.TEN, semester.getEndDate().plusDays(1)))))
                .andExpect(status().isBadRequest());
        var request = new EnterGrades(item.id(), List.of(new GradeEntry(student.getId(), BigDecimal.ONE, null)));
        grades.enterGrades(teacherUser.getId(), request);
        mvc.perform(post("/api/teacher/grades").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isConflict());
        assertThat(em.createQuery("select count(g.id) from StudentGrade g where g.gradeItem.id=:item", Long.class).setParameter("item", item.id()).getSingleResult()).isEqualTo(1);
    }

    @Test
    void batchRejectsForeignStudentWithoutSavingAnyGrade() throws Exception {
        GradeItemResponse item = item();
        var request = new EnterGrades(item.id(), List.of(new GradeEntry(student.getId(), BigDecimal.ONE, null), new GradeEntry(outsider.getId(), BigDecimal.ONE, null)));
        mvc.perform(post("/api/teacher/grades").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        assertThat(em.createQuery("select count(g.id) from StudentGrade g where g.gradeItem.id=:item", Long.class).setParameter("item", item.id()).getSingleResult()).isZero();
    }

    @Test
    void semesterMismatchAndUnassignedSubjectAreRejected() throws Exception {
        Subject extra = new Subject(); extra.setSchool(school); extra.setName("Văn"); extra.setSubjectCode("EXTRA"); extra.setActive(true); persist(extra);
        mvc.perform(get("/api/teacher/classes/{classId}/subjects/{subjectId}/grades", schoolClass.getId(), extra.getId())
                        .param("semesterId", semester.getId().toString()).header("Authorization", bearer(teacherUser, "TEACHER")))
                .andExpect(status().isForbidden());
        AcademicYear secondYear = new AcademicYear(); secondYear.setSchool(school);secondYear.setName("Other year");secondYear.setStartDate(today.minusYears(1));secondYear.setEndDate(today.minusMonths(6));persist(secondYear);
        Semester wrong = new Semester(); wrong.setAcademicYear(secondYear);wrong.setName("Other semester");wrong.setSemesterNumber(1);wrong.setStartDate(secondYear.getStartDate());wrong.setEndDate(secondYear.getEndDate());persist(wrong);
        mvc.perform(get("/api/teacher/classes/{classId}/subjects/{subjectId}/grades", schoolClass.getId(), subject.getId())
                        .param("semesterId", wrong.getId().toString()).header("Authorization", bearer(teacherUser, "TEACHER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void homeroomEvaluationPersistsAndParentReadsItButSubjectTeacherCannotEvaluate() throws Exception {
        EvaluationRequest request = new EvaluationRequest(student.getId(), schoolClass.getId(), semester.getId(), "Đạt", "Tốt", "Có tiến bộ");
        mvc.perform(post("/api/teacher/evaluations").header("Authorization", bearer(subjectUser, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        EvaluationResponse saved = grades.evaluate(teacherUser.getId(), request);
        em.flush();em.clear();
        assertThat(em.find(StudentEvaluation.class,saved.id()).getEvaluatedAt()).isNotNull();
        mvc.perform(get("/api/parent/children/{id}/evaluations", student.getId()).param("semesterId", semester.getId().toString())
                        .header("Authorization", bearer(parentUser,"PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].content").value("Có tiến bộ"));
        mvc.perform(post("/api/teacher/evaluations").header("Authorization", bearer(teacherUser,"TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void remarksHonorSubjectAssignmentAndParentVisibility() throws Exception {
        grades.remark(subjectUser.getId(), new RemarkRequest(student.getId(), schoolClass.getId(), semester.getId(), subjectAssignment.getId(), "Dành cho phụ huynh", today, RemarkVisibility.PARENTS_VISIBLE));
        grades.remark(subjectUser.getId(), new RemarkRequest(student.getId(), schoolClass.getId(), semester.getId(), subjectAssignment.getId(), "Ghi chú nội bộ", today, RemarkVisibility.STAFF_ONLY));
        em.flush();em.clear();
        mvc.perform(get("/api/parent/children/{id}/evaluations", student.getId()).param("semesterId", semester.getId().toString())
                        .header("Authorization", bearer(parentUser,"PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].content").value("Dành cho phụ huynh"));
        mvc.perform(post("/api/teacher/remarks").header("Authorization", bearer(subjectUser,"TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new RemarkRequest(student.getId(), schoolClass.getId(), semester.getId(), null, "Không có quyền chủ nhiệm", today, RemarkVisibility.PARENTS_VISIBLE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void homeroomSummaryReturnsStoredGradesAndEvaluationWithPagination() throws Exception {
        GradeItemResponse item = item();
        grades.enterGrades(teacherUser.getId(), new EnterGrades(item.id(), List.of(new GradeEntry(student.getId(), new BigDecimal("8"), null))));
        grades.evaluate(teacherUser.getId(), new EvaluationRequest(student.getId(), schoolClass.getId(), semester.getId(), null, null, "Nhận xét"));
        mvc.perform(get("/api/teacher/homeroom/academic-summary").header("Authorization", bearer(teacherUser, "TEACHER"))
                        .param("classId", schoolClass.getId().toString()).param("semesterId", semester.getId().toString()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].subjects[0].grades[0].score").value(8))
                .andExpect(jsonPath("$.data.content[0].evaluation.overallComment").value("Nhận xét"));
        mvc.perform(get("/api/teacher/homeroom/academic-summary").header("Authorization", bearer(subjectUser, "TEACHER"))
                        .param("classId", schoolClass.getId().toString()))
                .andExpect(status().isForbidden());
    }
}
