package com.example.myfschool.service.teacher;

import com.example.myfschool.dto.teacher.TeacherContextDtos.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import com.example.myfschool.service.TeacherService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TeacherServiceImpl implements TeacherService {
    private final EntityManager em;
    private final TeacherAccess access;
    public TeacherServiceImpl(EntityManager em,TeacherAccess access) {this.em=em;this.access=access;}
    private Ref ref(SchoolClass c){return new Ref(c.getId(),c.getClassName());}
    private Profile profile(Teacher t){return new Profile(t.getId(),t.getTeacherCode(),t.getFullName(),t.getPhone(),t.getEmail(),new Ref(t.getSchool().getId(),t.getSchool().getName()));}
    @Override public Profile profile(Long uid){return profile(access.teacher(uid));}
    private AcademicYear year(Teacher t,Long id){return id!=null?access.year(t,id):em.createQuery("select y from AcademicYear y where y.school.id=:s and y.isCurrent=true order by y.startDate desc,y.id",AcademicYear.class).setParameter("s",t.getSchool().getId()).setMaxResults(1).getResultStream().findFirst().orElse(null);}
    private Semester semester(Teacher t,AcademicYear y,Long id){
        if(id!=null){Semester s=access.semester(t,id);if(y!=null&&!Objects.equals(y.getId(),s.getAcademicYear().getId()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"Học kỳ không thuộc năm học");return s;}
        if(y==null)return null;
        List<Semester> ss=em.createQuery("select s from Semester s where s.academicYear.id=:y order by s.semesterNumber desc",Semester.class).setParameter("y",y.getId()).getResultList();
        return ss.stream().filter(s->!LocalDate.now().isBefore(s.getStartDate())&&!LocalDate.now().isAfter(s.getEndDate())).findFirst().orElse(ss.isEmpty()?null:ss.getFirst());
    }
    private List<SchoolClass> classes(Teacher t,AcademicYear y){
        return em.createQuery("select c from SchoolClass c join fetch c.school s join fetch c.academicYear y left join fetch c.homeroomTeacher where c.school.id=:school and y.school.id=:school and (:year is null or y.id=:year) and (c.homeroomTeacher.id=:teacher or exists(select a.id from TeachingAssignment a where a.schoolClass=c and a.teacher.id=:teacher and a.status=:active and a.subject.school.id=:school and a.semester.academicYear=y)) order by c.className,c.id",SchoolClass.class)
                .setParameter("school",t.getSchool().getId()).setParameter("year",y==null?null:y.getId()).setParameter("teacher",t.getId()).setParameter("active",ActiveStatus.ACTIVE).getResultList();
    }
    private List<TeachingAssignment> assignments(Teacher t,AcademicYear y,Semester s){
        return em.createQuery("select a from TeachingAssignment a join fetch a.schoolClass c join fetch c.academicYear y join fetch a.subject sub join fetch a.semester sem where a.teacher.id=:teacher and a.status=:active and c.school.id=:school and sub.school.id=:school and y.school.id=:school and sem.academicYear=y and (:year is null or y.id=:year) and (:sem is null or sem.id=:sem) order by sub.name,c.className,a.id",TeachingAssignment.class)
                .setParameter("teacher",t.getId()).setParameter("active",ActiveStatus.ACTIVE).setParameter("school",t.getSchool().getId()).setParameter("year",y==null?null:y.getId()).setParameter("sem",s==null?null:s.getId()).getResultList();
    }
    private Assignment assignmentDto(TeachingAssignment a){return new Assignment(a.getId(),new Ref(a.getSubject().getId(),a.getSubject().getName()),ref(a.getSchoolClass()),new Ref(a.getSemester().getId(),a.getSemester().getName()),new Ref(a.getSemester().getAcademicYear().getId(),a.getSemester().getAcademicYear().getName()));}
    private List<ClassInfo> classDtos(Teacher t,AcademicYear year){
        List<SchoolClass> cs=classes(t,year);if(cs.isEmpty())return List.of();
        Map<Long,List<Assignment>> as=assignments(t,year,null).stream().collect(Collectors.groupingBy(a->a.getSchoolClass().getId(),Collectors.mapping(this::assignmentDto,Collectors.toList())));
        Map<Long,Long> counts=new HashMap<>();
        em.createQuery("select e.schoolClass.id,count(distinct e.student.id) from ClassEnrollment e where e.schoolClass.id in :ids and e.student.school.id=:school and e.status=:active and e.enrolledAt<=:now and (e.leftAt is null or e.leftAt>:now) group by e.schoolClass.id",Object[].class)
                .setParameter("ids",cs.stream().map(SchoolClass::getId).toList()).setParameter("school",t.getSchool().getId()).setParameter("active",EnrollmentStatus.ACTIVE).setParameter("now",LocalDateTime.now()).getResultList().forEach(r->counts.put((Long)r[0],(Long)r[1]));
        return cs.stream().map(c->new ClassInfo(c.getId(),c.getClassName(),new Ref(c.getSchool().getId(),c.getSchool().getName()),new Ref(c.getAcademicYear().getId(),c.getAcademicYear().getName()),access.isHomeroom(t,c),counts.getOrDefault(c.getId(),0L),as.getOrDefault(c.getId(),List.of()))).toList();
    }
    @Override public List<ClassInfo> classes(Long uid,Long yearId){Teacher t=access.teacher(uid);return classDtos(t,year(t,yearId));}
    @Override public List<ClassInfo> homeroom(Long uid,Long yearId){return classes(uid,yearId).stream().filter(ClassInfo::homeroom).toList();}
    private StudentInfo studentDto(Student s,List<Ref> cs){return new StudentInfo(s.getId(),s.getStudentCode(),s.getFullName(),s.getDateOfBirth(),s.getGender(),s.getStatus(),new Ref(s.getSchool().getId(),s.getSchool().getName()),cs);}
    @Override public Page<StudentInfo> students(Long uid,Long classId,String keyword,int page,int size){
        TeacherAccess.page(page,size);Teacher t=access.teacher(uid);SchoolClass c=access.classAccess(t,classId);
        String search=keyword==null?"":keyword.trim().toLowerCase(Locale.ROOT);if(search.length()>150)throw new BusinessException(ErrorCode.INVALID_REQUEST);
        String where=" from Student s where s.school.id=:school and (locate(:q,lower(s.fullName))>0 or locate(:q,lower(s.studentCode))>0 or :q='') and exists(select e.id from ClassEnrollment e where e.student=s and e.schoolClass.id=:c and e.status=:active and e.enrolledAt<=:now and (e.leftAt is null or e.leftAt>:now))";
        var query=em.createQuery("select s"+where+" order by s.fullName,s.id",Student.class);var count=em.createQuery("select count(s)"+where,Long.class);
        for(var q:List.of(query,count))q.setParameter("school",t.getSchool().getId()).setParameter("q",search).setParameter("c",classId).setParameter("active",EnrollmentStatus.ACTIVE).setParameter("now",LocalDateTime.now());
        return new PageImpl<>(query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(s->studentDto(s,List.of(ref(c)))).toList(),PageRequest.of(page,size),count.getSingleResult());
    }
    private List<SchoolClass> studentClasses(Teacher t,Long studentId){
        Student s=access.require(Student.class,studentId);access.sameSchool(t,s.getSchool().getId());
        return em.createQuery("select distinct e.schoolClass from ClassEnrollment e where e.student.id=:s and e.status=:active and e.enrolledAt<=:now and (e.leftAt is null or e.leftAt>:now) order by e.schoolClass.id",SchoolClass.class)
                .setParameter("s",studentId).setParameter("active",EnrollmentStatus.ACTIVE).setParameter("now",LocalDateTime.now()).getResultList();
    }
    @Override public StudentInfo student(Long uid,Long id){Teacher t=access.teacher(uid);Set<Long> allowed=classes(t,null).stream().map(SchoolClass::getId).collect(Collectors.toSet());List<Ref> cs=studentClasses(t,id).stream().filter(c->allowed.contains(c.getId())).map(this::ref).toList();if(cs.isEmpty())throw new BusinessException(ErrorCode.ACCESS_DENIED);return studentDto(access.require(Student.class,id),cs);}
    @Override public List<Assignment> subjects(Long uid,Long yearId,Long semId){Teacher t=access.teacher(uid);AcademicYear y=semId!=null&&yearId==null?access.semester(t,semId).getAcademicYear():year(t,yearId);Semester s=semId==null?null:semester(t,y,semId);return assignments(t,y,s).stream().map(this::assignmentDto).toList();}
    @Override public List<Period> periods(Long uid){Teacher t=access.teacher(uid);return em.createQuery("select s from Semester s join fetch s.academicYear y where y.school.id=:school order by y.startDate desc,s.semesterNumber",Semester.class).setParameter("school",t.getSchool().getId()).getResultList().stream().map(s->new Period(s.getId(),s.getName(),s.getStartDate(),s.getEndDate(),new Ref(s.getAcademicYear().getId(),s.getAcademicYear().getName()))).toList();}
    @Override public List<Lesson> timetable(Long uid,Long yearId,Long semId,LocalDate date){
        Teacher t=access.teacher(uid);AcademicYear y=semId!=null&&yearId==null?access.semester(t,semId).getAcademicYear():year(t,yearId);Semester s=semester(t,y,semId);if(s==null)return List.of();LocalDate day=date==null?LocalDate.now():date;
        if(date!=null&&(day.isBefore(s.getStartDate())||day.isAfter(s.getEndDate())))throw new BusinessException(ErrorCode.INVALID_REQUEST,"Ngày không thuộc học kỳ");
        List<Long> ids=assignments(t,y,s).stream().map(TeachingAssignment::getId).toList();if(ids.isEmpty())return List.of();
        return em.createQuery("select e from TimetableEntry e join fetch e.teachingAssignment a join fetch a.schoolClass join fetch a.subject join fetch a.semester where a.id in :ids and e.effectiveFrom<=:d and (e.effectiveTo is null or e.effectiveTo>=:d) order by e.dayOfWeek,e.shift,e.periodNumber,e.id",TimetableEntry.class).setParameter("ids",ids).setParameter("d",day).getResultList().stream().map(e->new Lesson(e.getId(),e.getDayOfWeek(),e.getPeriodNumber(),e.getShift(),e.getRoom(),e.getEffectiveFrom(),e.getEffectiveTo(),assignmentDto(e.getTeachingAssignment()))).toList();
    }
    @Override public List<ParentContact> parents(Long uid,Long studentId){
        Teacher t=access.teacher(uid);boolean home=studentClasses(t,studentId).stream().anyMatch(c->Objects.equals(c.getSchool().getId(),t.getSchool().getId())&&access.isHomeroom(t,c));if(!home)throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return em.createQuery("select p from ParentStudent p join fetch p.parent par where p.student.id=:id and par.school.id=:s and par.user.school.id=:s order by p.isPrimaryContact desc,p.id",ParentStudent.class).setParameter("id",studentId).setParameter("s",t.getSchool().getId()).getResultList().stream().map(p->new ParentContact(p.getParent().getId(),p.getParent().getFullName(),p.getParent().getPhone(),p.getParent().getEmail(),p.getRelationshipType(),p.isPrimaryContact())).toList();
    }
    private long count(String query,Map<String,Object> params){var q=em.createQuery(query,Long.class);params.forEach(q::setParameter);return q.getSingleResult();}
    @Override public Activity activities(Long uid,LocalDate from,LocalDate to){
        Teacher t=access.teacher(uid);AcademicYear y=year(t,null);LocalDate start=from==null?(y==null?LocalDate.now().withDayOfYear(1):y.getStartDate()):from;LocalDate end=to==null?LocalDate.now():to;
        if(end.isBefore(start)||end.toEpochDay()-start.toEpochDay()>3660)throw new BusinessException(ErrorCode.INVALID_REQUEST,"Khoảng thời gian không hợp lệ");
        Map<String,Object> p=Map.of("u",uid,"s",t.getSchool().getId(),"from",start.atStartOfDay(),"to",end.plusDays(1).atStartOfDay());
        long sessions=count("select count(a) from AttendanceSession a where a.createdBy.id=:u and a.schoolClass.school.id=:s and a.createdAt>=:from and a.createdAt<:to",p);
        long completed=count("select count(a) from AttendanceSession a where a.createdBy.id=:u and a.schoolClass.school.id=:s and a.createdAt>=:from and a.createdAt<:to and a.status=com.example.myfschool.enums.AttendanceSessionStatus.COMPLETED",p);
        Map<String,Object> tp=Map.of("t",t.getId(),"s",t.getSchool().getId(),"from",start.atStartOfDay(),"to",end.plusDays(1).atStartOfDay());
        long items=count("select count(g) from GradeItem g where g.teachingAssignment.teacher.id=:t and g.teachingAssignment.schoolClass.school.id=:s and g.createdAt>=:from and g.createdAt<:to",tp);
        long grades=count("select count(g) from StudentGrade g where g.gradedBy.id=:u and g.student.school.id=:s and g.gradedAt>=:from and g.gradedAt<:to",p);
        long announcements=count("select count(a) from Announcement a where a.author.id=:u and a.school.id=:s and a.status=com.example.myfschool.enums.AnnouncementStatus.PUBLISHED and a.publishedAt>=:from and a.publishedAt<:to",p);
        long decisions=count("select count(l) from LeaveRequest l where l.reviewedBy.id=:u and l.school.id=:s and l.reviewedAt>=:from and l.reviewedAt<:to",p);
        List<TeachingAssignment> assignments=assignments(t,null,null).stream().filter(a->!a.getSemester().getEndDate().isBefore(start)&&!a.getSemester().getStartDate().isAfter(end)).toList();
        long classCount=classes(t,null).stream().filter(c->!c.getAcademicYear().getEndDate().isBefore(start)&&!c.getAcademicYear().getStartDate().isAfter(end)).count();
        return new Activity(start,end,classCount,assignments.stream().map(a->a.getSubject().getId()).distinct().count(),sessions,completed,items,grades,announcements,decisions);
    }
    @Override public Dashboard dashboard(Long uid,Long yearId){
        Teacher t=access.teacher(uid);AcademicYear y=year(t,yearId);List<ClassInfo> cs=classDtos(t,y);List<ClassInfo> home=cs.stream().filter(ClassInfo::homeroom).toList();List<Long> homes=home.stream().map(ClassInfo::id).toList();
        long pending=homes.isEmpty()?0:count("select count(l) from LeaveRequest l where l.school.id=:s and l.student.school.id=:s and l.schoolClass.id in :ids and l.status=com.example.myfschool.enums.LeaveRequestStatus.PENDING",Map.of("s",t.getSchool().getId(),"ids",homes));
        List<AnnouncementSummary> recent=em.createQuery("select a from Announcement a where a.school.id=:s and a.status=com.example.myfschool.enums.AnnouncementStatus.PUBLISHED and a.publishedAt<=:now and (a.expiresAt is null or a.expiresAt>:now) and (a.author.id=:u or exists(select target.id from AnnouncementTarget target where target.announcement=a and (target.user.id=:u or target.school.id=:s or target.schoolClass.id in :ids))) order by a.publishedAt desc,a.id desc",Announcement.class).setParameter("s",t.getSchool().getId()).setParameter("u",uid).setParameter("now",LocalDateTime.now()).setParameter("ids",cs.isEmpty()?List.of(-1L):cs.stream().map(ClassInfo::id).toList()).setMaxResults(5).getResultList().stream().map(a->new AnnouncementSummary(a.getId(),a.getTitle(),a.getPublishedAt())).toList();
        return new Dashboard(profile(t),y==null?null:new Ref(y.getId(),y.getName()),cs,home,timetable(uid,y==null?null:y.getId(),null,null).stream().filter(l->l.weekday()==LocalDate.now().getDayOfWeek().getValue()).toList(),pending,recent,activities(uid,y==null?null:y.getStartDate(),y==null?null:y.getEndDate()));
    }
}
