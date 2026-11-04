package com.example.myfschool.service;

import com.example.myfschool.dto.request.ParentDeviceRequest;
import com.example.myfschool.dto.request.ParentLeaveRequest;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.dto.response.ParentDashboardResponse;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parent-facing, authorization-scoped operations. EntityManager queries keep joins explicit. */
@Service
@Transactional(readOnly = true)
public class ParentService {
    private final ParentRepository parents;
    private final ParentStudentRepository parentStudents;
    private final LeaveRequestRepository leaveRequests;
    private final DeviceTokenRepository devices;
    private final AnnouncementReadRepository reads;
    private final LeaveRequestHistoryRepository leaveHistories;
    @PersistenceContext private EntityManager em;

    public ParentService(ParentRepository parents, ParentStudentRepository parentStudents,
            LeaveRequestRepository leaveRequests, DeviceTokenRepository devices,
            AnnouncementReadRepository reads, LeaveRequestHistoryRepository leaveHistories) {
        this.parents = parents; this.parentStudents = parentStudents; this.leaveRequests = leaveRequests;
        this.devices = devices; this.reads = reads; this.leaveHistories=leaveHistories;
    }

    private Parent parent(Long userId) {
        return parents.findByUser_Id(userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
    private Student child(Parent p, Long studentId) {
        ParentStudent link = parentStudents.findByParent_IdAndStudent_Id(p.getId(), studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        if (!Objects.equals(link.getStudent().getSchool().getId(), p.getSchool().getId()))
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return link.getStudent();
    }
    private ParentStudent link(Parent p, Long studentId) {
        child(p, studentId);
        return parentStudents.findByParent_IdAndStudent_Id(p.getId(), studentId).orElseThrow();
    }
    private void academicAllowed(Parent p, Long studentId) {
        if (!link(p, studentId).isCanViewAcademic()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
    private Map<String,Object> school(School s) { return map("id",s.getId(),"code",s.getSchoolCode(),"name",s.getName(),"address",s.getAddress(),"phone",s.getPhone()); }
    private Map<String,Object> teacher(Teacher t) { return t == null ? null : map("id",t.getId(),"fullName",t.getFullName(),"phone",t.getPhone(),"email",t.getEmail()); }
    private Map<String,Object> classContext(Student s) {
        List<ClassEnrollment> rows = em.createQuery("select e from ClassEnrollment e join fetch e.schoolClass c join fetch c.academicYear y left join fetch c.homeroomTeacher t where e.student.id=:sid and e.status=:status and c.status=:cs order by y.startDate desc", ClassEnrollment.class)
                .setParameter("sid",s.getId()).setParameter("status",EnrollmentStatus.ACTIVE).setParameter("cs",SchoolClassStatus.ACTIVE).setMaxResults(1).getResultList();
        if (rows.isEmpty()) return null;
        SchoolClass c = rows.get(0).getSchoolClass();
        return classDto(c);
    }
    private Map<String,Object> classDto(SchoolClass c){return map("id",c.getId(),"name",c.getClassName(),"gradeLevel",c.getGradeLevel(),"academicYear",map("id",c.getAcademicYear().getId(),"name",c.getAcademicYear().getName()),"homeroomTeacher",teacher(c.getHomeroomTeacher()));}
    private Map<String,Object> studentDto(Student s) {
        return map("id",s.getId(),"studentCode",s.getStudentCode(),"fullName",s.getFullName(),"dateOfBirth",s.getDateOfBirth(),"gender",s.getGender(),"status",s.getStatus(),"school",school(s.getSchool()),"class",classContext(s));
    }
    public List<Map<String,Object>> children(Long userId) {
        Parent p=parent(userId);
        List<ParentStudent> linked=parentStudents.findByParent_IdOrderByStudent_FullNameAsc(p.getId()).stream()
                .filter(x -> Objects.equals(x.getStudent().getSchool().getId(),p.getSchool().getId())).toList();
        if(linked.isEmpty())return List.of();
        List<Long> ids=linked.stream().map(x->x.getStudent().getId()).toList();
        List<ClassEnrollment> enrollments=em.createQuery("select e from ClassEnrollment e join fetch e.schoolClass c join fetch c.academicYear y left join fetch c.homeroomTeacher t where e.student.id in :ids and e.status=:status and c.status=:classStatus order by e.student.id,y.startDate desc",ClassEnrollment.class).setParameter("ids",ids).setParameter("status",EnrollmentStatus.ACTIVE).setParameter("classStatus",SchoolClassStatus.ACTIVE).getResultList();
        Map<Long,Map<String,Object>> classes=new HashMap<>();for(ClassEnrollment e:enrollments)classes.putIfAbsent(e.getStudent().getId(),classDto(e.getSchoolClass()));
        return linked.stream().map(x -> map("id",x.getStudent().getId(),"fullName",x.getStudent().getFullName(),"studentCode",x.getStudent().getStudentCode(),"avatar",null,"school",school(x.getStudent().getSchool()),"class",classes.get(x.getStudent().getId()),"enrollmentStatus",x.getStudent().getStatus())).toList();
    }
    public Map<String,Object> childContext(Long userId,Long sid) { return studentDto(child(parent(userId),sid)); }
    public Map<String,Object> classInfo(Long userId,Long sid) { return classContext(child(parent(userId),sid)); }

    public ParentDashboardResponse dashboard(Long userId,Long requestedStudent) {
        Parent p=parent(userId); List<Map<String,Object>> kids=children(userId);
        Long sid=requestedStudent;
        if (sid==null && !kids.isEmpty()) sid=(Long)kids.getFirst().get("id");
        Student selected=sid==null?null:child(p,sid);
        List<Map<String,Object>> recent=List.of();Map<String,Object> attendance=emptyAttendance();List<Map<String,Object>> academic=List.of();
        if(selected!=null){
            recent=announcementSummaries(visibleAnnouncements(p,userId,PageRequest.of(0,5,Sort.by(Sort.Direction.DESC,"publishedAt"))).getContent(),userId);
            if(link(p,sid).isCanViewAcademic()){
                attendance=attendanceSummary(p,userId,sid,null,null);
                academic=gradeSummary(p,userId,sid,null,null);
            }
        }
        return new ParentDashboardResponse(map("id",p.getId(),"fullName",p.getFullName(),"phone",p.getPhone(),"email",p.getEmail()),kids,selected==null?null:studentDto(selected),recent,attendance,academic,leaveRequests.countByParent_IdAndStatus(p.getId(),LeaveRequestStatus.PENDING));
    }

    public List<Map<String,Object>> timetable(Long userId,Long sid,Long yearId,Long semesterId) {
        Parent p=parent(userId); academicAllowed(p,sid); Student s=child(p,sid);
        AcademicYear y=yearId==null?em.createQuery("select y from AcademicYear y where y.school.id=:school and y.isCurrent=true",AcademicYear.class).setParameter("school",p.getSchool().getId()).setMaxResults(1).getResultStream().findFirst().orElse(null):em.find(AcademicYear.class,yearId);
        if(y==null||!Objects.equals(y.getSchool().getId(),p.getSchool().getId()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"Academic year is outside this school");
        Semester sem=resolveForStudent(s,y.getId(),semesterId);
        List<Long> classes=em.createQuery("select e.schoolClass.id from ClassEnrollment e where e.student.id=:sid and e.schoolClass.academicYear.id=:year and e.status=:status",Long.class).setParameter("sid",sid).setParameter("year",y.getId()).setParameter("status",EnrollmentStatus.ACTIVE).getResultList();
        if(classes.isEmpty())return List.of();
        LocalDate today=LocalDate.now();
        return em.createQuery("select t from TimetableEntry t join fetch t.teachingAssignment a join fetch a.subject sub join fetch a.teacher te where a.schoolClass.id in :classes and a.status=:active and a.semester.id=:sem and t.effectiveFrom<=:today and (t.effectiveTo is null or t.effectiveTo>=:today) order by t.dayOfWeek,t.periodNumber",TimetableEntry.class)
                .setParameter("classes",classes).setParameter("active",ActiveStatus.ACTIVE).setParameter("sem",sem.getId()).setParameter("today",today).getResultList().stream()
                .map(t->{TeachingAssignment a=t.getTeachingAssignment();return map("weekday",t.getDayOfWeek(),"period",t.getPeriodNumber(),"shift",t.getShift(),"subject",map("id",a.getSubject().getId(),"code",a.getSubject().getSubjectCode(),"name",a.getSubject().getName()),"teacher",teacher(a.getTeacher()),"startTime",null,"endTime",null,"classroom",t.getRoom());}).toList();
    }
    public List<Map<String,Object>> academicResults(Long userId,Long sid,Long yearId,Long semesterId) {
        Parent p=parent(userId); academicAllowed(p,sid); Student s=child(p,sid); Semester sem=resolveForStudent(s,yearId,semesterId);
        List<StudentGrade> grades=em.createQuery("select g from StudentGrade g join fetch g.gradeItem gi join fetch gi.teachingAssignment a join fetch a.subject sub where g.student.id=:sid and a.semester.id=:sem order by sub.name,gi.assessmentDate,gi.id",StudentGrade.class).setParameter("sid",sid).setParameter("sem",sem.getId()).getResultList();
        Map<Long,List<StudentGrade>> grouped=grades.stream().collect(Collectors.groupingBy(g->g.getGradeItem().getTeachingAssignment().getSubject().getId(),LinkedHashMap::new,Collectors.toList()));
        return grouped.values().stream().map(gs->{StudentGrade first=gs.getFirst();Subject sub=first.getGradeItem().getTeachingAssignment().getSubject();return map("subject",map("id",sub.getId(),"code",sub.getSubjectCode(),"name",sub.getName()),"grades",gs.stream().map(this::gradeDto).toList());}).toList();
    }
    public List<Map<String,Object>> subjectGrades(Long userId,Long sid,Long subjectId,Long semesterId) {
        Parent p=parent(userId);academicAllowed(p,sid);Student s=child(p,sid);Semester sem=resolveForStudent(s,null,semesterId);
        Subject requested=em.find(Subject.class,subjectId);if(requested==null||!Objects.equals(requested.getSchool().getId(),s.getSchool().getId()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"Subject is outside this student's school");
        Long matchingAssignments=em.createQuery("select count(a.id) from TeachingAssignment a where a.subject.id=:subject and a.semester.id=:sem and a.schoolClass.id in (select e.schoolClass.id from ClassEnrollment e where e.student.id=:sid)",Long.class).setParameter("subject",subjectId).setParameter("sem",sem.getId()).setParameter("sid",sid).getSingleResult();if(matchingAssignments==0)throw new BusinessException(ErrorCode.INVALID_REQUEST,"Subject does not match this student's semester/class");
        return em.createQuery("select g from StudentGrade g join fetch g.gradeItem gi join fetch gi.teachingAssignment a join fetch a.subject sub where g.student.id=:sid and sub.id=:sub and a.semester.id=:sem order by gi.assessmentDate,gi.id",StudentGrade.class).setParameter("sid",sid).setParameter("sub",subjectId).setParameter("sem",sem.getId()).getResultList().stream().map(this::gradeDto).toList();
    }
    private Map<String,Object> gradeDto(StudentGrade g){GradeItem i=g.getGradeItem();return map("gradeItemId",i.getId(),"name",i.getTitle(),"type",i.getGradeType(),"score",g.getScore(),"weight",i.getWeight(),"assessmentDate",i.getAssessmentDate(),"comment",g.getComment());}
    private List<Map<String,Object>> gradeSummary(Parent p,Long userId,Long sid,Long y,Long sem){try{return academicResults(userId,sid,y,sem);}catch(BusinessException e){return List.of();}}

    private record EvaluationRow(LocalDateTime sortTime,Map<String,Object> value) {}
    public Map<String,Object> evaluations(Long userId,Long sid,Long yearId,Long semesterId,int page,int size){Parent p=parent(userId);academicAllowed(p,sid);Student s=child(p,sid);Semester sem=resolveForStudent(s,yearId,semesterId);validatePage(page,size);int limit=Math.multiplyExact(page+1,size);
        List<EvaluationRow> rows=new ArrayList<>();
        em.createQuery("select r from StudentRemark r join fetch r.teacher t where r.student.id=:sid and r.semester.id=:sem and r.visibility=:vis order by r.remarkDate desc,r.id desc",StudentRemark.class).setParameter("sid",sid).setParameter("sem",sem.getId()).setParameter("vis",RemarkVisibility.PARENTS_VISIBLE).setMaxResults(limit).getResultList().forEach(r->rows.add(new EvaluationRow(r.getRemarkDate().atStartOfDay(),map("type","REMARK","teacher",teacher(r.getTeacher()),"subject",null,"content",r.getContent(),"date",r.getRemarkDate()))));
        em.createQuery("select e from StudentEvaluation e left join fetch e.evaluatedBy where e.student.id=:sid and e.semester.id=:sem order by e.evaluatedAt desc,e.id desc",StudentEvaluation.class).setParameter("sid",sid).setParameter("sem",sem.getId()).setMaxResults(limit).getResultList().forEach(e->{LocalDateTime when=e.getEvaluatedAt()==null?e.getCreatedAt():e.getEvaluatedAt();rows.add(new EvaluationRow(when,map("type","EVALUATION","teacher",null,"subject",null,"content",e.getOverallComment(),"academicResult",e.getAcademicResult(),"conductResult",e.getConductResult(),"date",e.getEvaluatedAt())));});
        rows.sort(Comparator.comparing(EvaluationRow::sortTime,Comparator.nullsLast(Comparator.reverseOrder())));int start=Math.min(rows.size(),Math.multiplyExact(page,size));int end=Math.min(rows.size(),start+size);long total=em.createQuery("select count(r.id) from StudentRemark r where r.student.id=:sid and r.semester.id=:sem and r.visibility=:vis",Long.class).setParameter("sid",sid).setParameter("sem",sem.getId()).setParameter("vis",RemarkVisibility.PARENTS_VISIBLE).getSingleResult()+em.createQuery("select count(e.id) from StudentEvaluation e where e.student.id=:sid and e.semester.id=:sem",Long.class).setParameter("sid",sid).setParameter("sem",sem.getId()).getSingleResult();return page(rows.subList(start,end).stream().map(EvaluationRow::value).toList(),page,size,total);}

    public Map<String,Object> attendance(Long userId,Long sid,LocalDate from,LocalDate to,int page,int size){Parent p=parent(userId);academicAllowed(p,sid);child(p,sid);validatePage(page,size);if(from!=null&&to!=null&&to.isBefore(from))throw new BusinessException(ErrorCode.INVALID_REQUEST,"to must be on or after from");
        String clause="r.student.id=:sid and ss.status<>:cancelled"+(from==null?"":" and ss.sessionDate>=:from")+(to==null?"":" and ss.sessionDate<=:to");
        var query=em.createQuery("select r from AttendanceRecord r join fetch r.session ss where "+clause+" order by ss.sessionDate desc,ss.periodNumber desc",AttendanceRecord.class).setParameter("sid",sid);if(from!=null)query.setParameter("from",from);if(to!=null)query.setParameter("to",to);
        query.setParameter("cancelled",AttendanceSessionStatus.CANCELLED);int offset=Math.multiplyExact(page,size);List<AttendanceRecord> rows=query.setFirstResult(offset).setMaxResults(size).getResultList();var count=em.createQuery("select count(r.id) from AttendanceRecord r join r.session ss where "+clause,Long.class).setParameter("sid",sid).setParameter("cancelled",AttendanceSessionStatus.CANCELLED);if(from!=null)count.setParameter("from",from);if(to!=null)count.setParameter("to",to);
        List<Map<String,Object>> data=rows.stream().map(r->map("date",r.getSession().getSessionDate(),"sessionType",r.getSession().getSessionType(),"shift",r.getSession().getShift(),"period",r.getSession().getPeriodNumber(),"status",r.getStatus(),"remarks",r.getNote())).toList();return page(data,page,size,count.getSingleResult());}
    public Map<String,Object> attendanceSummary(Parent p,Long userId,Long sid,Long yearId,Long semId){academicAllowed(p,sid);Student s=child(p,sid);LocalDate start=null,end=null;if(semId!=null){Semester sem=resolveForStudent(s,yearId,semId);start=sem.getStartDate();end=sem.getEndDate();}else if(yearId!=null){AcademicYear y=em.find(AcademicYear.class,yearId);if(y==null||!Objects.equals(y.getSchool().getId(),p.getSchool().getId()))throw new BusinessException(ErrorCode.INVALID_REQUEST);start=y.getStartDate();end=y.getEndDate();}
        String q="select r.status,count(r.id) from AttendanceRecord r join r.session ss where r.student.id=:sid and ss.status<>:cancelled"+(start==null?"":" and ss.sessionDate>=:start and ss.sessionDate<=:end")+" group by r.status";var query=em.createQuery(q,Object[].class).setParameter("sid",sid).setParameter("cancelled",AttendanceSessionStatus.CANCELLED);if(start!=null)query.setParameter("start",start).setParameter("end",end);Map<AttendanceStatus,Long> counts=new EnumMap<>(AttendanceStatus.class);query.getResultList().forEach(x->counts.put((AttendanceStatus)x[0],(Long)x[1]));long total=counts.values().stream().mapToLong(Long::longValue).sum();return map("totalSessions",total,"present",counts.getOrDefault(AttendanceStatus.PRESENT,0L),"absent",counts.getOrDefault(AttendanceStatus.ABSENT,0L),"excused",counts.getOrDefault(AttendanceStatus.EXCUSED,0L),"late",counts.getOrDefault(AttendanceStatus.LATE,0L));}
    private Map<String,Object> emptyAttendance(){return map("totalSessions",0,"present",0,"absent",0,"excused",0,"late",0);}

    public Map<String,Object> announcements(Long userId,int page,int size){Parent p=parent(userId);validatePage(page,size);Page<Announcement> result=visibleAnnouncements(p,userId,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"publishedAt")));return page(announcementSummaries(result.getContent(),userId),page,size,result.getTotalElements());}
    private Page<Announcement> visibleAnnouncements(Parent p,Long userId,PageRequest pageable){List<Long> classes=classIds(p);String clause="a.school.id=:school and a.status=:published and a.publishedAt is not null and a.publishedAt<=:now and (a.expiresAt is null or a.expiresAt>:now) and exists (select t.id from AnnouncementTarget t where t.announcement=a and ((t.targetType=:schoolType and t.school.id=:school) or (t.targetType=:userType and t.user.id=:uid) or (t.targetType=:classType and t.schoolClass.id in :classes)))";var q=em.createQuery("select distinct a from Announcement a where "+clause+" order by a.publishedAt desc",Announcement.class).setParameter("school",p.getSchool().getId()).setParameter("published",AnnouncementStatus.PUBLISHED).setParameter("now",LocalDateTime.now()).setParameter("schoolType",AnnouncementTargetType.SCHOOL).setParameter("userType",AnnouncementTargetType.USER).setParameter("classType",AnnouncementTargetType.CLASS).setParameter("uid",userId).setParameter("classes",classes.isEmpty()?List.of(-1L):classes);List<Announcement> rows=q.setFirstResult((int)pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();Long total=em.createQuery("select count(distinct a.id) from Announcement a where "+clause,Long.class).setParameter("school",p.getSchool().getId()).setParameter("published",AnnouncementStatus.PUBLISHED).setParameter("now",LocalDateTime.now()).setParameter("schoolType",AnnouncementTargetType.SCHOOL).setParameter("userType",AnnouncementTargetType.USER).setParameter("classType",AnnouncementTargetType.CLASS).setParameter("uid",userId).setParameter("classes",classes.isEmpty()?List.of(-1L):classes).getSingleResult();return new org.springframework.data.domain.PageImpl<>(rows,pageable,total);}
    private List<Long> classIds(Parent p){return em.createQuery("select distinct e.schoolClass.id from ClassEnrollment e where e.student.id in (select ps.student.id from ParentStudent ps where ps.parent.id=:pid) and e.status=:st and e.schoolClass.school.id=:school",Long.class).setParameter("pid",p.getId()).setParameter("st",EnrollmentStatus.ACTIVE).setParameter("school",p.getSchool().getId()).getResultList();}
    private Map<String,Object> announcementSummary(Announcement a,Long userId){boolean read=reads.findByAnnouncement_IdAndUser_Id(a.getId(),userId).isPresent();return map("id",a.getId(),"title",a.getTitle(),"preview",a.getContent().length()>240?a.getContent().substring(0,240):a.getContent(),"publishedAt",a.getPublishedAt(),"sender",a.getAuthor().getUsername(),"read",read);}
    private List<Map<String,Object>> announcementSummaries(List<Announcement> announcements,Long userId){if(announcements.isEmpty())return List.of();Set<Long> readIds=reads.findByUser_IdAndAnnouncement_IdIn(userId,announcements.stream().map(Announcement::getId).toList()).stream().map(r->r.getAnnouncement().getId()).collect(Collectors.toSet());return announcements.stream().map(a->map("id",a.getId(),"title",a.getTitle(),"preview",a.getContent().length()>240?a.getContent().substring(0,240):a.getContent(),"publishedAt",a.getPublishedAt(),"sender",a.getAuthor().getUsername(),"read",readIds.contains(a.getId()))).toList();}
    public Map<String,Object> announcement(Long userId,Long id){Parent p=parent(userId);Announcement a=visibleAnnouncement(p,userId,id);Map<String,Object> out=announcementSummary(a,userId);out.put("content",a.getContent());return out;}
    private Announcement visibleAnnouncement(Parent p,Long userId,Long id){List<Long> classes=classIds(p);return em.createQuery("select a from Announcement a where a.id=:id and a.school.id=:school and a.status=:status and a.publishedAt is not null and a.publishedAt<=:now and (a.expiresAt is null or a.expiresAt>:now) and exists (select t.id from AnnouncementTarget t where t.announcement=a and ((t.targetType=:st and t.school.id=:school) or (t.targetType=:ut and t.user.id=:uid) or (t.targetType=:ct and t.schoolClass.id in :classes)))",Announcement.class).setParameter("id",id).setParameter("school",p.getSchool().getId()).setParameter("status",AnnouncementStatus.PUBLISHED).setParameter("now",LocalDateTime.now()).setParameter("st",AnnouncementTargetType.SCHOOL).setParameter("ut",AnnouncementTargetType.USER).setParameter("ct",AnnouncementTargetType.CLASS).setParameter("uid",userId).setParameter("classes",classes.isEmpty()?List.of(-1L):classes).getResultStream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    @Transactional public void markRead(Long userId,Long id){Parent p=parent(userId);visibleAnnouncement(p,userId,id);reads.insertIfAbsent(id,userId);}

    @Transactional public Map<String,Object> submitLeave(Long userId,Long sid,ParentLeaveRequest req){Parent p=parent(userId);ParentStudent rel=link(p,sid);if(!rel.isCanSubmitLeave())throw new BusinessException(ErrorCode.ACCESS_DENIED);if(req.endDate().isBefore(req.startDate()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"endDate must be on or after startDate");Student s=child(p,sid);SchoolClass c=currentClass(s);if(c==null)throw new BusinessException(ErrorCode.INVALID_REQUEST,"Student has no current class");if(leaveRequests.existsByStudent_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(sid,LeaveRequestStatus.PENDING,req.endDate(),req.startDate()))throw new BusinessException(ErrorCode.RESOURCE_CONFLICT,"A pending leave request overlaps these dates");LeaveRequest lr=new LeaveRequest();lr.setParent(p);lr.setStudent(s);lr.setSchool(s.getSchool());lr.setSchoolClass(c);lr.setStartDate(req.startDate());lr.setEndDate(req.endDate());lr.setReason(req.reason().trim());lr.setStatus(LeaveRequestStatus.PENDING);lr=leaveRequests.save(lr);LeaveRequestHistory h=new LeaveRequestHistory();h.setLeaveRequest(lr);h.setActor(p.getUser());h.setOldStatus(null);h.setNewStatus(LeaveRequestStatus.PENDING);h.setNote("Parent submitted request");em.persist(h);return leaveDto(lr);}
    public Map<String,Object> leave(Long userId,Long id){Parent p=parent(userId);LeaveRequest lr=leaveRequests.findByIdAndParent_Id(id,p.getId()).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));return leaveDto(lr);}
    public Map<String,Object> attendanceSummaryFor(Long userId,Long sid,Long yearId,Long semesterId){Parent p=parent(userId);return attendanceSummary(p,userId,sid,yearId,semesterId);}
    public Map<String,Object> leaveHistory(Long userId,Long sid,LeaveRequestStatus status,int page,int size){Parent p=parent(userId);child(p,sid);validatePage(page,size);PageRequest pg=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"));Page<LeaveRequest> result=status==null?leaveRequests.findByParent_IdAndStudent_Id(p.getId(),sid,pg):leaveRequests.findByParent_IdAndStudent_IdAndStatus(p.getId(),sid,status,pg);List<Long> ids=result.getContent().stream().map(LeaveRequest::getId).toList();Map<Long,List<LeaveRequestHistory>> histories=ids.isEmpty()?Map.of():leaveHistories.findByLeaveRequest_IdInOrderByChangedAtAsc(ids).stream().collect(Collectors.groupingBy(h->h.getLeaveRequest().getId()));return page(result.getContent().stream().map(l->leaveDto(l,histories.getOrDefault(l.getId(),List.of()))).toList(),page,size,result.getTotalElements());}
    private Map<String,Object> leaveDto(LeaveRequest l){return leaveDto(l,leaveHistories.findByLeaveRequest_IdOrderByChangedAtAsc(l.getId()));}
    private Map<String,Object> leaveDto(LeaveRequest l,List<LeaveRequestHistory> history){List<Map<String,Object>> histories=history.stream().map(h->map("from",h.getOldStatus(),"to",h.getNewStatus(),"changedAt",h.getChangedAt(),"note",h.getNote())).toList();return map("id",l.getId(),"student",map("id",l.getStudent().getId(),"fullName",l.getStudent().getFullName()),"startDate",l.getStartDate(),"endDate",l.getEndDate(),"reason",l.getReason(),"status",l.getStatus(),"createdAt",l.getCreatedAt(),"decisionAt",l.getReviewedAt(),"reviewer",l.getReviewedBy()==null?null:l.getReviewedBy().getUsername(),"rejectionReason",l.getStatus()==LeaveRequestStatus.REJECTED?l.getReviewNote():null,"history",histories);}
    @Transactional public void registerDevice(Long userId,ParentDeviceRequest request){parent(userId);DeviceToken token=devices.findByToken(request.token()).orElseGet(DeviceToken::new);if(token.getId()!=null&&!Objects.equals(token.getUser().getId(),userId))throw new BusinessException(ErrorCode.RESOURCE_CONFLICT,"Device token is already registered to another account");token.setUser(em.getReference(User.class,userId));token.setToken(request.token());token.setPlatform(request.platform());token.setDeviceName(request.deviceName());token.setActive(true);devices.save(token);}
    @Transactional public void removeDevice(Long userId,String token){parent(userId);devices.findByTokenAndUser_Id(token,userId).ifPresent(d->{d.setActive(false);devices.save(d);});}

    private Semester resolveForStudent(Student s,Long yearId,Long semesterId){if(semesterId!=null){Semester sem=em.find(Semester.class,semesterId);if(sem==null||!Objects.equals(sem.getAcademicYear().getSchool().getId(),s.getSchool().getId())||(yearId!=null&&!Objects.equals(sem.getAcademicYear().getId(),yearId)))throw new BusinessException(ErrorCode.INVALID_REQUEST,"Semester does not belong to requested school/year");return sem;}AcademicYear y=yearId==null?em.createQuery("select y from AcademicYear y where y.school.id=:school and y.isCurrent=true",AcademicYear.class).setParameter("school",s.getSchool().getId()).setMaxResults(1).getResultStream().findFirst().orElse(null):em.find(AcademicYear.class,yearId);if(y==null||!Objects.equals(y.getSchool().getId(),s.getSchool().getId()))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"No matching academic year");LocalDate today=LocalDate.now();return em.createQuery("select sem from Semester sem where sem.academicYear.id=:year order by case when :today between sem.startDate and sem.endDate then 0 else 1 end,sem.semesterNumber desc",Semester.class).setParameter("year",y.getId()).setParameter("today",today).setMaxResults(1).getResultStream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"No semester found"));}
    private SchoolClass currentClass(Student s){return em.createQuery("select e.schoolClass from ClassEnrollment e where e.student.id=:sid and e.status=:st and e.schoolClass.status=:cs order by e.schoolClass.academicYear.startDate desc",SchoolClass.class).setParameter("sid",s.getId()).setParameter("st",EnrollmentStatus.ACTIVE).setParameter("cs",SchoolClassStatus.ACTIVE).setMaxResults(1).getResultStream().findFirst().orElse(null);}
    private void validatePage(int page,int size){if(page<0||page>1_000_000||size<1||size>100)throw new BusinessException(ErrorCode.INVALID_REQUEST,"page must be between 0 and 1000000 and size must be between 1 and 100");}
    private Map<String,Object> page(List<?> content,int page,int size,long total){return map("content",content,"page",page,"size",size,"totalElements",total,"totalPages",(int)Math.ceil((double)total/size));}
    private Map<String,Object> map(Object... pairs){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
}
