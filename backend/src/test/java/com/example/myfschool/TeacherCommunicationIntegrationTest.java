package com.example.myfschool;

import static com.example.myfschool.dto.teacher.TeacherCommunicationDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myfschool.dto.request.ParentLeaveRequest;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.ParentService;
import com.example.myfschool.service.teacher.TeacherCommunicationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;

class TeacherCommunicationIntegrationTest extends TeacherIntegrationSupport {
    @Autowired TeacherCommunicationService service;
    @Autowired ParentService parentService;

    @Test
    void draftIsPrivatePublishedClassTargetIsVisibleAndReadStatisticsAreReal() throws Exception {
        AnnouncementResponse draft = draft();
        assertThat(draft.status()).isEqualTo(AnnouncementStatus.DRAFT);
        assertThat(draft.createdAt()).isNotNull();
        mvc.perform(get("/api/parent/announcements/" + draft.id())
                .header("Authorization", bearer(parentUser, "PARENT"))).andExpect(status().isNotFound());

        mvc.perform(post("/api/teacher/announcements/" + draft.id() + "/publish")
                .header("Authorization", bearer(teacherUser, "TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
        em.flush(); em.clear();
        mvc.perform(get("/api/parent/announcements/" + draft.id())
                .header("Authorization", bearer(parentUser, "PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Thông báo lớp"))
                .andExpect(jsonPath("$.data.read").value(false));
        assertThat(service.reads(teacherUser.getId(), draft.id(), 0, 20).readCount()).isZero();
        parentService.markRead(parentUser.getId(), draft.id());
        parentService.markRead(parentUser.getId(), draft.id());
        AnnouncementReads reads = service.reads(teacherUser.getId(), draft.id(), 0, 20);
        assertThat(reads.recipientCount()).isEqualTo(1);
        assertThat(reads.readCount()).isEqualTo(1);
        assertThat(reads.recipients().content()).singleElement().satisfies(r -> {
            assertThat(r.read()).isTrue(); assertThat(r.readAt()).isNotNull();
        });
        LocalDateTime published = service.announcement(teacherUser.getId(), draft.id()).publishedAt();
        assertThat(service.publish(teacherUser.getId(), draft.id()).publishedAt()).isEqualTo(published);
        assertThat(jdbc.queryForObject("select count(*) from announcement_targets where announcement_id=?", Long.class, draft.id()))
                .isEqualTo(1);
        assertThat(service.announcements(teacherUser.getId(), schoolClass.getId(), AnnouncementStatus.PUBLISHED, 0, 20)
                .content()).extracting(AnnouncementResponse::id).containsExactly(draft.id());
    }

    @Test
    void recipientsAreDeduplicatedAndUnrelatedParentsCannotSeeAnnouncement() {
        ParentStudent secondLink = new ParentStudent(); secondLink.setParent(parent);secondLink.setStudent(secondStudent);
        secondLink.setRelationshipType(RelationshipType.GUARDIAN); persist(secondLink);
        User unrelatedUser = user(school, "unrelated-parent", "PARENT");
        Parent unrelated = new Parent();unrelated.setUser(unrelatedUser);unrelated.setSchool(school);unrelated.setFullName("Unrelated");persist(unrelated);
        AnnouncementResponse announcement = service.publish(teacherUser.getId(), draft().id());
        assertThat(service.reads(teacherUser.getId(), announcement.id(), 0, 20).recipientCount()).isEqualTo(1);
        assertThatThrownBy(() -> parentService.announcement(unrelatedUser.getId(), announcement.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void subjectTeacherAndOtherSchoolCannotCreatePublishOrReadHomeroomAnnouncements() throws Exception {
        AnnouncementResponse announcement = draft();
        for (User unauthorized : List.of(subjectUser, otherUser)) {
            mvc.perform(post("/api/teacher/announcements").header("Authorization", bearer(unauthorized,"TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"classId\":" + schoolClass.getId()
                            + ",\"title\":\"Notice\",\"content\":\"Text\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/teacher/announcements/" + announcement.id() + "/publish")
                    .header("Authorization", bearer(unauthorized,"TEACHER"))).andExpect(status().isForbidden());
            mvc.perform(get("/api/teacher/announcements/" + announcement.id() + "/reads")
                    .header("Authorization", bearer(unauthorized,"TEACHER"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void announcementValidationRejectsBlankContentBadDatesAndPagination() throws Exception {
        mvc.perform(post("/api/teacher/announcements").header("Authorization", bearer(teacherUser,"TEACHER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"classId\":" + schoolClass.getId()
                        + ",\"title\":\" \",\"content\":\"Text\"}"))
                .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> service.createAnnouncement(teacherUser.getId(),
                new CreateAnnouncement(schoolClass.getId(),"Title","Text",LocalDateTime.now().minusDays(1))))
                .isInstanceOf(BusinessException.class);
        mvc.perform(get("/api/teacher/announcements?size=101").header("Authorization",bearer(teacherUser,"TEACHER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parentSubmissionIsListedDecidedAndReturnedWithPersistedReviewerHistory() throws Exception {
        Long id = submitLeave();
        assertThat(service.leaves(teacherUser.getId(), schoolClass.getId(), LeaveRequestStatus.PENDING,
                today, today.plusDays(2), 0, 20).content()).extracting(LeaveResponse::id).containsExactly(id);
        mvc.perform(patch("/api/teacher/leave-requests/" + id + "/decision")
                .header("Authorization", bearer(teacherUser,"TEACHER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\",\"reason\":\"Đồng ý\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewer.id").value(teacherUser.getId()));
        em.flush(); em.clear();
        mvc.perform(get("/api/parent/leave-requests/" + id).header("Authorization", bearer(parentUser,"PARENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.decisionAt").isNotEmpty()).andExpect(jsonPath("$.data.history.length()").value(2));
        assertThat(service.history(teacherUser.getId(), id)).hasSize(2).last().satisfies(h -> {
            assertThat(h.from()).isEqualTo(LeaveRequestStatus.PENDING);
            assertThat(h.to()).isEqualTo(LeaveRequestStatus.APPROVED);
            assertThat(h.actor().id()).isEqualTo(teacherUser.getId());assertThat(h.changedAt()).isNotNull();
        });
        mvc.perform(patch("/api/teacher/leave-requests/" + id + "/decision")
                .header("Authorization", bearer(teacherUser,"TEACHER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unauthorizedTeachersCannotReadOrDecideLeaveAndInvalidTransitionsAreRejected() throws Exception {
        Long id = submitLeave();
        for (User unauthorized : List.of(subjectUser, otherUser)) {
            mvc.perform(get("/api/teacher/leave-requests/"+id).header("Authorization",bearer(unauthorized,"TEACHER")))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/teacher/leave-requests/"+id+"/history").header("Authorization",bearer(unauthorized,"TEACHER")))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/teacher/leave-requests/"+id+"/decision").header("Authorization",bearer(unauthorized,"TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                    .andExpect(status().isForbidden());
            assertThat(service.leaves(unauthorized.getId(), null,null,null,null,0,20).content()).isEmpty();
        }
        for (String invalid : List.of("{\"decision\":\"PENDING\"}","{\"decision\":\"CANCELLED\"}","{\"decision\":\"REJECTED\"}")) {
            mvc.perform(patch("/api/teacher/leave-requests/"+id+"/decision").header("Authorization",bearer(teacherUser,"TEACHER"))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        assertThat(service.leave(teacherUser.getId(), id).status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(service.history(teacherUser.getId(), id)).hasSize(1);
    }

    @Test
    void rejectionReasonPersistsForParent() {
        Long id = submitLeave();
        service.decide(teacherUser.getId(), id, new LeaveDecision(LeaveRequestStatus.REJECTED,"Cần bổ sung giấy tờ"));
        em.flush();em.clear();
        assertThat(parentService.leave(parentUser.getId(), id)).containsEntry("status",LeaveRequestStatus.REJECTED)
                .containsEntry("rejectionReason","Cần bổ sung giấy tờ");
    }

    @Test
    void concurrentDecisionsCommitExactlyOneTransition() throws Exception {
        Long id = submitLeave();
        Long userId = teacherUser.getId();
        em.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<ErrorCode> approve = () -> decideConcurrently(start,userId,id,LeaveRequestStatus.APPROVED);
            Callable<ErrorCode> reject = () -> decideConcurrently(start,userId,id,LeaveRequestStatus.REJECTED);
            var first = executor.submit(approve);var second = executor.submit(reject);start.countDown();
            assertThat(java.util.Arrays.asList(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(null,ErrorCode.RESOURCE_CONFLICT);
        }
        TestTransaction.start();
        assertThat(service.history(userId,id)).hasSize(2);
        assertThat(service.leave(userId,id).status()).isIn(LeaveRequestStatus.APPROVED,LeaveRequestStatus.REJECTED);
        assertThat(service.leave(userId,id).decisionAt()).isNotNull();
    }

    private ErrorCode decideConcurrently(CountDownLatch start, Long uid, Long id, LeaveRequestStatus status) throws InterruptedException {
        start.await();
        try { service.decide(uid,id,new LeaveDecision(status,"Concurrent review")); return null; }
        catch (BusinessException e) { return e.getErrorCode(); }
    }
    private AnnouncementResponse draft() {
        return service.createAnnouncement(teacherUser.getId(), new CreateAnnouncement(schoolClass.getId(),
                "Thông báo lớp","Nội dung gửi phụ huynh",null));
    }
    private Long submitLeave() {
        return (Long) parentService.submitLeave(parentUser.getId(), student.getId(),
                new ParentLeaveRequest(today, today.plusDays(1), "Học sinh bị ốm")).get("id");
    }
}
