package com.example.myfschool.service.teacher;

import static com.example.myfschool.dto.teacher.TeacherCommunicationDtos.*;

import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherCommunicationService {
    private final EntityManager em;
    private final TeacherAccess access;

    public TeacherCommunicationService(EntityManager em, TeacherAccess access) {
        this.em = em;
        this.access = access;
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(Long userId, CreateAnnouncement request) {
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass = access.homeroom(teacher, request.classId());
        activeClass(schoolClass);
        if (request.expiresAt() != null && !request.expiresAt().isAfter(LocalDateTime.now())) {
            throw invalid("Thời hạn thông báo phải ở tương lai");
        }
        Announcement announcement = new Announcement();
        announcement.setSchool(teacher.getSchool());
        announcement.setAuthor(teacher.getUser());
        announcement.setTitle(requiredText(request.title(), 255));
        announcement.setContent(requiredText(request.content(), 100_000));
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setExpiresAt(request.expiresAt());
        em.persist(announcement);
        AnnouncementTarget target = new AnnouncementTarget();
        target.setAnnouncement(announcement);
        target.setTargetType(AnnouncementTargetType.CLASS);
        target.setSchoolClass(schoolClass);
        em.persist(target);
        em.flush();
        em.refresh(announcement);
        return announcementDto(announcement, schoolClass);
    }

    @Transactional
    public AnnouncementResponse publish(Long userId, Long announcementId) {
        Teacher teacher = access.teacher(userId);
        Announcement announcement = ownedAnnouncement(teacher, announcementId, true);
        SchoolClass schoolClass = announcementClass(teacher, announcement);
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            return announcementDto(announcement, schoolClass);
        }
        if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
            throw conflict("Chỉ có thể phát hành thông báo nháp");
        }
        activeClass(schoolClass);
        LocalDateTime now = LocalDateTime.now();
        if (announcement.getExpiresAt() != null && !announcement.getExpiresAt().isAfter(now)) {
            throw conflict("Thông báo đã quá thời hạn phát hành");
        }
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(now);
        em.flush();
        return announcementDto(announcement, schoolClass);
    }

    public AnnouncementResponse announcement(Long userId, Long announcementId) {
        Teacher teacher = access.teacher(userId);
        Announcement announcement = ownedAnnouncement(teacher, announcementId, false);
        return announcementDto(announcement, announcementClass(teacher, announcement));
    }

    public PageResponse<AnnouncementResponse> announcements(Long userId, Long classId,
            AnnouncementStatus status, int page, int size) {
        validatePage(page, size);
        Teacher teacher = access.teacher(userId);
        if (classId != null) access.homeroom(teacher, classId);
        String where = "a.author.id=:uid and a.school.id=:school and (select count(x.id) from AnnouncementTarget x where x.announcement=a)=1 "
                + "and exists (select t.id from AnnouncementTarget t "
                + "where t.announcement=a and t.targetType=:target and t.schoolClass.homeroomTeacher.id=:teacher "
                + "and t.schoolClass.school.id=:school and t.schoolClass.status=:active"
                + (classId == null ? "" : " and t.schoolClass.id=:classId") + ")"
                + (status == null ? "" : " and a.status=:status");
        var query = em.createQuery("select a from Announcement a join fetch a.author where " + where
                + " order by a.createdAt desc,a.id desc", Announcement.class);
        var count = em.createQuery("select count(a.id) from Announcement a where " + where, Long.class);
        for (TypedQuery<?> q : List.of(query, count)) {
            q.setParameter("uid", userId).setParameter("school", teacher.getSchool().getId())
                    .setParameter("teacher", teacher.getId()).setParameter("target", AnnouncementTargetType.CLASS)
                    .setParameter("active", SchoolClassStatus.ACTIVE);
            if (classId != null) q.setParameter("classId", classId);
            if (status != null) q.setParameter("status", status);
        }
        List<Announcement> rows = query.setFirstResult(page * size).setMaxResults(size).getResultList();
        Map<Long, SchoolClass> targets = targetClasses(rows);
        return PageResponse.of(rows.stream().map(a -> announcementDto(a, targets.get(a.getId()))).toList(),
                page, size, count.getSingleResult());
    }

    /** CLASS targets follow current class membership, exactly as ParentService visibility does. */
    public AnnouncementReads reads(Long userId, Long announcementId, int page, int size) {
        validatePage(page, size);
        Teacher teacher = access.teacher(userId);
        Announcement announcement = ownedAnnouncement(teacher, announcementId, false);
        SchoolClass schoolClass = announcementClass(teacher, announcement);
        String recipients = "p.school.id=:school and p.user.school.id=:school and exists (select ps.id from ParentStudent ps "
                + "where ps.parent=p and ps.student.school.id=:school and exists (select e.id from ClassEnrollment e "
                + "where e.student=ps.student and e.schoolClass.id=:classId and e.status=:active))";
        var parents = em.createQuery("select p from Parent p join fetch p.user where " + recipients
                + " order by p.fullName,p.id", Parent.class);
        var count = em.createQuery("select count(p.id) from Parent p where " + recipients, Long.class);
        var readCount = em.createQuery("select count(p.id) from Parent p where " + recipients
                + " and exists (select r.id from AnnouncementRead r where r.announcement.id=:aid and r.user=p.user)", Long.class)
                .setParameter("aid", announcementId);
        for (TypedQuery<?> q : List.of(parents, count, readCount)) {
            q.setParameter("school", teacher.getSchool().getId()).setParameter("classId", schoolClass.getId())
                    .setParameter("active", EnrollmentStatus.ACTIVE);
        }
        List<Parent> rows = parents.setFirstResult(page * size).setMaxResults(size).getResultList();
        List<Long> users = rows.stream().map(p -> p.getUser().getId()).toList();
        Map<Long, AnnouncementRead> reads = users.isEmpty() ? Map.of() : em.createQuery(
                "select r from AnnouncementRead r where r.announcement.id=:aid and r.user.id in :users", AnnouncementRead.class)
                .setParameter("aid", announcementId).setParameter("users", users).getResultStream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), Function.identity()));
        List<RecipientRead> data = rows.stream().map(p -> {
            AnnouncementRead read = reads.get(p.getUser().getId());
            return new RecipientRead(p.getId(), p.getFullName(), read != null, read == null ? null : read.getReadAt());
        }).toList();
        long total = count.getSingleResult();
        return new AnnouncementReads(announcementId, total, readCount.getSingleResult(),
                PageResponse.of(data, page, size, total));
    }

    public PageResponse<LeaveResponse> leaves(Long userId, Long classId, LeaveRequestStatus status,
            LocalDate from, LocalDate to, int page, int size) {
        validatePage(page, size);
        if (from != null && to != null && to.isBefore(from)) throw invalid("Khoảng ngày không hợp lệ");
        Teacher teacher = access.teacher(userId);
        if (classId != null) access.homeroom(teacher, classId);
        String where = "l.school.id=:school and l.student.school.id=:school and l.parent.school.id=:school "
                + "and l.schoolClass.school.id=:school and l.schoolClass.homeroomTeacher.id=:teacher "
                + "and l.schoolClass.status=:active"
                + (classId == null ? "" : " and l.schoolClass.id=:classId")
                + (status == null ? "" : " and l.status=:status")
                + (from == null ? "" : " and l.endDate>=:from")
                + (to == null ? "" : " and l.startDate<=:to");
        var query = em.createQuery("select l from LeaveRequest l join fetch l.student join fetch l.parent "
                + "join fetch l.schoolClass left join fetch l.reviewedBy where " + where
                + " order by l.createdAt desc,l.id desc", LeaveRequest.class);
        var count = em.createQuery("select count(l.id) from LeaveRequest l where " + where, Long.class);
        for (TypedQuery<?> q : List.of(query, count)) {
            q.setParameter("school", teacher.getSchool().getId()).setParameter("teacher", teacher.getId())
                    .setParameter("active", SchoolClassStatus.ACTIVE);
            if (classId != null) q.setParameter("classId", classId);
            if (status != null) q.setParameter("status", status);
            if (from != null) q.setParameter("from", from);
            if (to != null) q.setParameter("to", to);
        }
        return PageResponse.of(query.setFirstResult(page * size).setMaxResults(size).getResultStream()
                .map(this::leaveDto).toList(), page, size, count.getSingleResult());
    }

    public LeaveResponse leave(Long userId, Long requestId) {
        return leaveDto(authorizedLeave(access.teacher(userId), requestId, false));
    }

    @Transactional
    public LeaveResponse decide(Long userId, Long requestId, LeaveDecision decision) {
        Teacher teacher = access.teacher(userId);
        if (decision.decision() != LeaveRequestStatus.APPROVED && decision.decision() != LeaveRequestStatus.REJECTED) {
            throw invalid("Quyết định phải là APPROVED hoặc REJECTED");
        }
        String note = decision.reason() == null ? null : decision.reason().trim();
        if (note != null && note.length() > 5000) throw invalid("Lý do quá dài");
        if (decision.decision() == LeaveRequestStatus.REJECTED && (note == null || note.isBlank())) {
            throw invalid("Vui lòng nhập lý do từ chối");
        }
        LeaveRequest request = authorizedLeave(teacher, requestId, true);
        activeClass(request.getSchoolClass());
        if (request.getStatus() != LeaveRequestStatus.PENDING) throw conflict("Đơn đã được xử lý");
        request.setStatus(decision.decision());
        request.setReviewedBy(teacher.getUser());
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(note);
        LeaveRequestHistory history = new LeaveRequestHistory();
        history.setLeaveRequest(request);
        history.setActor(teacher.getUser());
        history.setOldStatus(LeaveRequestStatus.PENDING);
        history.setNewStatus(decision.decision());
        history.setNote(note);
        em.persist(history);
        em.flush();
        return leaveDto(request);
    }

    public List<HistoryResponse> history(Long userId, Long requestId) {
        authorizedLeave(access.teacher(userId), requestId, false);
        return em.createQuery("select h from LeaveRequestHistory h join fetch h.actor where "
                + "h.leaveRequest.id=:id order by h.changedAt,h.id", LeaveRequestHistory.class)
                .setParameter("id", requestId).getResultStream()
                .map(h -> new HistoryResponse(h.getId(), h.getOldStatus(), h.getNewStatus(),
                        actor(h.getActor()), h.getChangedAt(), h.getNote())).toList();
    }

    private Announcement ownedAnnouncement(Teacher teacher, Long id, boolean lock) {
        Announcement announcement = lock ? em.find(Announcement.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(Announcement.class, id);
        if (announcement == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!Objects.equals(announcement.getSchool().getId(), teacher.getSchool().getId())
                || !Objects.equals(announcement.getAuthor().getId(), teacher.getUser().getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return announcement;
    }

    private SchoolClass announcementClass(Teacher teacher, Announcement announcement) {
        List<AnnouncementTarget> targets = em.createQuery(
                "select t from AnnouncementTarget t left join fetch t.schoolClass where t.announcement=:announcement",
                AnnouncementTarget.class).setParameter("announcement", announcement).getResultList();
        // Teacher-created announcements have exactly one CLASS target; never widen an existing audience.
        if (targets.size() != 1 || targets.getFirst().getTargetType() != AnnouncementTargetType.CLASS) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return access.homeroom(teacher, targets.getFirst().getSchoolClass().getId());
    }

    private Map<Long, SchoolClass> targetClasses(List<Announcement> announcements) {
        if (announcements.isEmpty()) return Map.of();
        return em.createQuery("select t from AnnouncementTarget t join fetch t.schoolClass where "
                + "t.announcement.id in :ids and t.targetType=:type", AnnouncementTarget.class)
                .setParameter("ids", announcements.stream().map(Announcement::getId).toList())
                .setParameter("type", AnnouncementTargetType.CLASS).getResultStream()
                .collect(Collectors.toMap(t -> t.getAnnouncement().getId(), AnnouncementTarget::getSchoolClass, (a, b) -> a));
    }

    private LeaveRequest authorizedLeave(Teacher teacher, Long id, boolean lock) {
        LeaveRequest request = lock ? em.find(LeaveRequest.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(LeaveRequest.class, id);
        if (request == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Long schoolId = teacher.getSchool().getId();
        if (!Objects.equals(request.getSchool().getId(), schoolId)
                || !Objects.equals(request.getStudent().getSchool().getId(), schoolId)
                || !Objects.equals(request.getParent().getSchool().getId(), schoolId)
                || !Objects.equals(request.getParent().getUser().getSchool().getId(), schoolId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        SchoolClass schoolClass = access.homeroom(teacher, request.getSchoolClass().getId());
        access.enrolled(teacher, schoolClass, request.getStudent().getId(), request.getStartDate());
        return request;
    }

    private AnnouncementResponse announcementDto(Announcement a, SchoolClass c) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(), a.getStatus(),
                new ClassReference(c.getId(), c.getClassName()), actor(a.getAuthor()), a.getCreatedAt(),
                a.getPublishedAt(), a.getExpiresAt());
    }

    private LeaveResponse leaveDto(LeaveRequest l) {
        return new LeaveResponse(l.getId(), new StudentReference(l.getStudent().getId(),
                l.getStudent().getStudentCode(), l.getStudent().getFullName()),
                new ClassReference(l.getSchoolClass().getId(), l.getSchoolClass().getClassName()),
                l.getParent().getFullName(), l.getStartDate(), l.getEndDate(), l.getReason(), l.getStatus(),
                l.getCreatedAt(), actor(l.getReviewedBy()), l.getReviewedAt(), l.getReviewNote());
    }

    private Actor actor(User user) { return user == null ? null : new Actor(user.getId(), user.getUsername()); }
    private void activeClass(SchoolClass schoolClass) {
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) throw conflict("Lớp học đã được lưu trữ");
    }
    private void validatePage(int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) throw invalid("Phân trang không hợp lệ");
    }
    private String requiredText(String value, int limit) {
        if (value == null || value.isBlank() || value.length() > limit) throw invalid("Nội dung không hợp lệ");
        return value.trim();
    }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_REQUEST, message); }
    private BusinessException conflict(String message) { return new BusinessException(ErrorCode.RESOURCE_CONFLICT, message); }
}
