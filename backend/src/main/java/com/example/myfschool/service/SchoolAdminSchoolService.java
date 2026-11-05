package com.example.myfschool.service;

import com.example.myfschool.dto.AdminSchoolDtos.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import jakarta.persistence.*;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SchoolAdminSchoolService {
    private final EntityManager em;
    private final SchoolAdminAccountService accounts;
    private final Validator validator;
    private final SchoolAdminAuditService audit;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private static final Map<String,Class<?>> TYPES=Map.of("teachers",Teacher.class,"students",Student.class,
        "parents",Parent.class,"academic-years",AcademicYear.class,"semesters",Semester.class,
        "subjects",Subject.class,"classes",SchoolClass.class,"teaching-assignments",TeachingAssignment.class);
    private void invalid(){throw new BusinessException(ErrorCode.INVALID_REQUEST);}
    private void conflict(){throw new BusinessException(ErrorCode.RESOURCE_CONFLICT);}
    private void validate(Object r){if(!validator.validate(r).isEmpty())invalid();}
    private School school(Object x){
        if(x instanceof Teacher v)return v.getSchool();if(x instanceof Student v)return v.getSchool();
        if(x instanceof Parent v)return v.getSchool();if(x instanceof AcademicYear v)return v.getSchool();
        if(x instanceof Semester v)return v.getAcademicYear().getSchool();if(x instanceof Subject v)return v.getSchool();
        if(x instanceof SchoolClass v)return v.getSchool();if(x instanceof TeachingAssignment v)return v.getSchoolClass().getSchool();
        if(x instanceof User v)return v.getSchool();throw new IllegalArgumentException();
    }
    private <T> T require(User a,Class<T> type,Long id){
        T x=em.find(type,id);if(x==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!school(x).getId().equals(a.getSchool().getId()))throw new BusinessException(ErrorCode.ACCESS_DENIED);return x;
    }
    private User writer(Long uid){User a=accounts.admin(uid);em.lock(a.getSchool(),LockModeType.PESSIMISTIC_WRITE);return a;}
    private Map<String,Object> map(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
    private Map<String,Object> view(Object x){
        if(x instanceof Teacher t)return map("id",t.getId(),"userId",t.getUser().getId(),"teacherCode",t.getTeacherCode(),"fullName",t.getFullName(),"dateOfBirth",t.getDateOfBirth(),"gender",t.getGender(),"phone",t.getPhone(),"email",t.getEmail(),"status",t.getStatus());
        if(x instanceof Student s)return map("id",s.getId(),"studentCode",s.getStudentCode(),"fullName",s.getFullName(),"dateOfBirth",s.getDateOfBirth(),"gender",s.getGender(),"admissionDate",s.getAdmissionDate(),"address",s.getAddress(),"status",s.getStatus());
        if(x instanceof Parent p)return map("id",p.getId(),"userId",p.getUser().getId(),"fullName",p.getFullName(),"phone",p.getPhone(),"email",p.getEmail(),"address",p.getAddress());
        if(x instanceof AcademicYear y)return map("id",y.getId(),"name",y.getName(),"startDate",y.getStartDate(),"endDate",y.getEndDate(),"current",y.isCurrent());
        if(x instanceof Semester s)return map("id",s.getId(),"academicYearId",s.getAcademicYear().getId(),"semesterNumber",s.getSemesterNumber(),"name",s.getName(),"startDate",s.getStartDate(),"endDate",s.getEndDate());
        if(x instanceof Subject s)return map("id",s.getId(),"subjectCode",s.getSubjectCode(),"name",s.getName(),"description",s.getDescription(),"active",s.isActive());
        if(x instanceof SchoolClass c)return map("id",c.getId(),"academicYearId",c.getAcademicYear().getId(),"className",c.getClassName(),"gradeLevel",c.getGradeLevel(),"homeroomTeacherId",c.getHomeroomTeacher()==null?null:c.getHomeroomTeacher().getId(),"status",c.getStatus());
        if(x instanceof TeachingAssignment a)return map("id",a.getId(),"classId",a.getSchoolClass().getId(),"teacherId",a.getTeacher().getId(),"subjectId",a.getSubject().getId(),"semesterId",a.getSemester().getId(),"status",a.getStatus());
        throw new IllegalArgumentException();
    }
    private String scope(String resource){return switch(resource){case "semesters"->"x.academicYear.school.id";case "teaching-assignments"->"x.schoolClass.school.id";default->"x.school.id";};}
    public Map<String,Object> list(Long uid,String resource,String keyword,Long academicYearId,int page,int size){
        User a=accounts.admin(uid);SchoolAdminAccountService.page(page,size);Class<?> type=TYPES.get(resource);if(type==null)invalid();
        String q=keyword==null?"":keyword.trim().toLowerCase(Locale.ROOT);if(q.length()>150)invalid();
        String name=switch(resource){case "teachers","students","parents"->"fullName";case "classes"->"className";case "teaching-assignments"->null;default->"name";};
        String where=" where "+scope(resource)+"=:school";
        if(name!=null)where+=" and locate(:q,lower(x."+name+"))>0";
        String year=switch(resource){case "semesters","classes"->"x.academicYear.id";case "teaching-assignments"->"x.semester.academicYear.id";default->null;};
        if(academicYearId!=null){require(a,AcademicYear.class,academicYearId);if(year!=null)where+=" and "+year+"=:year";}
        var rows=em.createQuery("select x from "+type.getSimpleName()+" x"+where+" order by x.id",type);var count=em.createQuery("select count(x) from "+type.getSimpleName()+" x"+where,Long.class);
        for(var query:List.of(rows,count)){query.setParameter("school",a.getSchool().getId());if(name!=null)query.setParameter("q",q);if(academicYearId!=null&&year!=null)query.setParameter("year",academicYearId);}
        return map("content",rows.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(this::view).toList(),"page",page,"size",size,"totalElements",count.getSingleResult());
    }
    public Map<String,Object> get(Long uid,String resource,Long id){Class<?> type=TYPES.get(resource);if(type==null)invalid();return view(require(accounts.admin(uid),type,id));}
    @Transactional public Map<String,Object> teacher(Long uid,Long id,TeacherProfile r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_TEACHER","Teacher",id);Teacher t=require(a,Teacher.class,id);
        if(em.createQuery("select count(t) from Teacher t where t.school.id=:s and t.teacherCode=:c and t.id<>:id",Long.class).setParameter("s",a.getSchool().getId()).setParameter("c",r.teacherCode().trim()).setParameter("id",id).getSingleResult()>0)conflict();
        t.setTeacherCode(r.teacherCode().trim());t.setFullName(r.fullName().trim());t.setDateOfBirth(r.dateOfBirth());t.setGender(r.gender());t.setPhone(r.phone());t.setEmail(r.email());t.setStatus(r.status());t.getUser().setPhone(r.phone());t.getUser().setEmail(r.email());return view(t);}
    @Transactional public Map<String,Object> student(Long uid,Long id,StudentProfile r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_STUDENT","Student",id);
        if(r.dateOfBirth()!=null&&r.admissionDate()!=null&&r.admissionDate().isBefore(r.dateOfBirth()))invalid();
        if(em.createQuery("select count(s) from Student s where s.school.id=:school and s.studentCode=:code and (:id is null or s.id<>:id)",Long.class).setParameter("school",a.getSchool().getId()).setParameter("code",r.studentCode().trim()).setParameter("id",id).getSingleResult()>0)conflict();
        Student s=id==null?new Student():require(a,Student.class,id);s.setSchool(a.getSchool());s.setStudentCode(r.studentCode().trim());s.setFullName(r.fullName().trim());s.setDateOfBirth(r.dateOfBirth());s.setGender(r.gender());s.setAdmissionDate(r.admissionDate());s.setAddress(r.address());s.setStatus(r.status());if(id==null)em.persist(s);em.flush();return view(s);}
    @Transactional public Map<String,Object> parent(Long uid,Long id,ParentProfile r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_PARENT","Parent",id);
        User u;
        if(r.userId()==null){
            if(id!=null||r.username()==null||r.username().isBlank()||!r.username().equals(r.username().trim())||r.password()==null||r.password().isBlank()||r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)invalid();
            if(em.createQuery("select count(u) from User u where lower(u.username)=:n",Long.class).setParameter("n",r.username().toLowerCase(Locale.ROOT)).getSingleResult()>0)conflict();
            u=new User();u.setSchool(a.getSchool());u.setUsername(r.username());u.setPasswordHash(encoder.encode(r.password()));u.setStatus(UserStatus.ACTIVE);u.setMustChangePassword(true);em.persist(u);
        }else{if(r.username()!=null||r.password()!=null)invalid();u=require(a,User.class,r.userId());}
        Parent p=id==null?new Parent():require(a,Parent.class,id);
        if(id!=null&&!p.getUser().getId().equals(r.userId()))invalid();
        if(id==null&&em.createQuery("select count(p) from Parent p where p.user.id=:u",Long.class).setParameter("u",r.userId()).getSingleResult()>0)conflict();
        p.setSchool(a.getSchool());p.setUser(u);p.setFullName(r.fullName().trim());p.setPhone(r.phone());p.setEmail(r.email());p.setAddress(r.address());u.setPhone(r.phone());u.setEmail(r.email());if(id==null)em.persist(p);
        if(em.createQuery("select count(r) from UserRole r where r.user.id=:u and r.role.code='PARENT'",Long.class).setParameter("u",u.getId()).getSingleResult()==0){Role role=em.createQuery("select r from Role r where r.code='PARENT'",Role.class).getSingleResult();UserRole ur=new UserRole();ur.setUser(u);ur.setRole(role);em.persist(ur);}em.flush();return view(p);}
    private ParentStudent link(User a,Long parentId,Long studentId){require(a,Parent.class,parentId);require(a,Student.class,studentId);return em.createQuery("select l from ParentStudent l where l.parent.id=:p and l.student.id=:s",ParentStudent.class).setParameter("p",parentId).setParameter("s",studentId).getResultStream().findFirst().orElse(null);}
    @Transactional public Map<String,Object> link(Long uid,Long parentId,Long studentId,ParentLink r,boolean create){validate(r);User a=writer(uid);audit.record(a,"ADMIN_LINK","ParentStudent",null);ParentStudent l=link(a,parentId,studentId);if(create&&l!=null)conflict();if(!create&&l==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(l==null){l=new ParentStudent();l.setParent(require(a,Parent.class,parentId));l.setStudent(require(a,Student.class,studentId));em.persist(l);}l.setRelationshipType(r.relationshipType());l.setPrimaryContact(r.primaryContact());l.setCanViewAcademic(r.canViewAcademic());l.setCanSubmitLeave(r.canSubmitLeave());em.flush();return linkView(l);}
    private Map<String,Object> linkView(ParentStudent l){return map("id",l.getId(),"parentId",l.getParent().getId(),"studentId",l.getStudent().getId(),"relationshipType",l.getRelationshipType(),"primaryContact",l.isPrimaryContact(),"canViewAcademic",l.isCanViewAcademic(),"canSubmitLeave",l.isCanSubmitLeave());}
    public Map<String,Object> links(Long uid,Long parentId,int page,int size){User a=accounts.admin(uid);require(a,Parent.class,parentId);SchoolAdminAccountService.page(page,size);var rows=em.createQuery("select l from ParentStudent l join fetch l.student where l.parent.id=:p and l.student.school.id=:s order by l.id",ParentStudent.class).setParameter("p",parentId).setParameter("s",a.getSchool().getId()).setFirstResult(page*size).setMaxResults(size).getResultList();long total=em.createQuery("select count(l) from ParentStudent l where l.parent.id=:p and l.student.school.id=:s",Long.class).setParameter("p",parentId).setParameter("s",a.getSchool().getId()).getSingleResult();return map("content",rows.stream().map(this::linkView).toList(),"page",page,"size",size,"totalElements",total);}
    @Transactional public void unlink(Long uid,Long parentId,Long studentId){User a=writer(uid);audit.record(a,"UNLINK_PARENT","Student",studentId);ParentStudent l=link(a,parentId,studentId);if(l==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);em.remove(l);}
    private void dates(LocalDate start,LocalDate end){if(!end.isAfter(start))invalid();}
    @Transactional public Map<String,Object> year(Long uid,Long id,Year r){validate(r);dates(r.startDate(),r.endDate());User a=writer(uid);audit.record(a,"ADMIN_YEAR","AcademicYear",id);AcademicYear y=id==null?new AcademicYear():require(a,AcademicYear.class,id);
        if(id!=null&&em.createQuery("select count(s) from Semester s where s.academicYear.id=:id and (s.startDate<:start or s.endDate>:end)",Long.class).setParameter("id",id).setParameter("start",r.startDate()).setParameter("end",r.endDate()).getSingleResult()>0)invalid();
        if(id!=null&&(!r.startDate().equals(y.getStartDate())||!r.endDate().equals(y.getEndDate()))&&em.createQuery("select count(c) from SchoolClass c where c.academicYear.id=:id",Long.class).setParameter("id",id).getSingleResult()>0)conflict();
        if(r.current())em.createQuery("update AcademicYear y set y.isCurrent=false where y.school.id=:s").setParameter("s",a.getSchool().getId()).executeUpdate();y.setSchool(a.getSchool());y.setName(r.name().trim());y.setStartDate(r.startDate());y.setEndDate(r.endDate());y.setCurrent(r.current());if(id==null)em.persist(y);em.flush();return view(y);}
    @Transactional public Map<String,Object> term(Long uid,Long id,Term r){validate(r);dates(r.startDate(),r.endDate());User a=writer(uid);audit.record(a,"ADMIN_TERM","Semester",id);AcademicYear y=require(a,AcademicYear.class,r.academicYearId());if(r.startDate().isBefore(y.getStartDate())||r.endDate().isAfter(y.getEndDate()))invalid();Semester s=id==null?new Semester():require(a,Semester.class,id);
        if(id!=null&&!s.getAcademicYear().getId().equals(y.getId()))invalid();
        if(id!=null&&(!r.startDate().equals(s.getStartDate())||!r.endDate().equals(s.getEndDate()))&&em.createQuery("select count(a) from TeachingAssignment a where a.semester.id=:id",Long.class).setParameter("id",id).getSingleResult()>0)conflict();
        s.setAcademicYear(y);s.setSemesterNumber(r.semesterNumber());s.setName(r.name().trim());s.setStartDate(r.startDate());s.setEndDate(r.endDate());if(id==null)em.persist(s);em.flush();return view(s);}
    @Transactional public Map<String,Object> subject(Long uid,Long id,SubjectProfile r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_SUBJECT","Subject",id);Subject s=id==null?new Subject():require(a,Subject.class,id);s.setSchool(a.getSchool());s.setSubjectCode(r.subjectCode().trim());s.setName(r.name().trim());s.setDescription(r.description());s.setActive(r.active());if(id==null)em.persist(s);em.flush();return view(s);}
    @Transactional public Map<String,Object> schoolClass(Long uid,Long id,ClassProfile r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_SCHOOLCLASS","SchoolClass",id);AcademicYear y=require(a,AcademicYear.class,r.academicYearId());SchoolClass c=id==null?new SchoolClass():require(a,SchoolClass.class,id);if(id!=null&&!c.getAcademicYear().getId().equals(y.getId()))invalid();c.setSchool(a.getSchool());c.setAcademicYear(y);c.setClassName(r.className().trim());c.setGradeLevel(r.gradeLevel());c.setStatus(r.status());if(id==null)em.persist(c);em.flush();return view(c);}
    @Transactional public Map<String,Object> homeroom(Long uid,Long classId,Homeroom r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_HOMEROOM","SchoolClass",classId);SchoolClass c=require(a,SchoolClass.class,classId);active(c);Teacher t=r.teacherId()==null?null:require(a,Teacher.class,r.teacherId());if(t!=null)activeTeacher(t);c.setHomeroomTeacher(t);return view(c);}
    private void active(SchoolClass c){if(c.getStatus()!=SchoolClassStatus.ACTIVE)conflict();}
    private void enrollmentDate(SchoolClass c,LocalDate d){if(d.isBefore(c.getAcademicYear().getStartDate())||d.isAfter(c.getAcademicYear().getEndDate()))invalid();}
    private void addEnrollment(User a,SchoolClass c,Student student,LocalDate date){
        active(c);enrollmentDate(c,date);if(student.getStatus()!=StudentStatus.ACTIVE)invalid();
        long overlaps=((Number)em.createNativeQuery("select count(*) from class_enrollments e join school_classes c on c.id=e.class_id where e.student_id=:s and c.academic_year_id=:y and (e.left_at is null or e.left_at>=:d) and e.enrolled_at<=:end")
            .setParameter("s",student.getId()).setParameter("y",c.getAcademicYear().getId()).setParameter("d",date).setParameter("end",c.getAcademicYear().getEndDate()).getSingleResult()).longValue();if(overlaps>0)conflict();
        em.createNativeQuery("insert into class_enrollments(class_id,student_id,enrolled_at,status) values (:c,:s,:d,'ACTIVE')").setParameter("c",c.getId()).setParameter("s",student.getId()).setParameter("d",date).executeUpdate();
    }
    @Transactional public Map<String,Object> enroll(Long uid,Long classId,Enrollment r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_ENROLL","SchoolClass",classId);SchoolClass c=require(a,SchoolClass.class,classId);Student s=require(a,Student.class,r.studentId());em.lock(s,LockModeType.PESSIMISTIC_WRITE);addEnrollment(a,c,s,r.enrolledAt());return map("classId",classId,"studentId",s.getId(),"enrolledAt",r.enrolledAt());}
    private ClassEnrollment enrollment(User a,Long id){ClassEnrollment e=em.find(ClassEnrollment.class,id);if(e==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);require(a,SchoolClass.class,e.getSchoolClass().getId());require(a,Student.class,e.getStudent().getId());return e;}
    @Transactional public Map<String,Object> move(Long uid,Long enrollmentId,Move r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_MOVE","ClassEnrollment",enrollmentId);ClassEnrollment e=enrollment(a,enrollmentId);em.lock(e.getStudent(),LockModeType.PESSIMISTIC_WRITE);SchoolClass target=require(a,SchoolClass.class,r.targetClassId());SchoolClass source=e.getSchoolClass();active(source);
        if(source.getId().equals(target.getId())||!source.getAcademicYear().getId().equals(target.getAcademicYear().getId())||e.getStatus()!=EnrollmentStatus.ACTIVE||e.getLeftAt()!=null||!r.effectiveDate().isAfter(e.getEnrolledAt().toLocalDate()))invalid();
        enrollmentDate(target,r.effectiveDate());
        long records=em.createQuery("select count(r) from AttendanceRecord r where r.student.id=:s and r.session.schoolClass.id=:c and r.session.sessionDate>=:d",Long.class).setParameter("s",e.getStudent().getId()).setParameter("c",source.getId()).setParameter("d",r.effectiveDate()).getSingleResult();
        long grades=em.createQuery("select count(g) from StudentGrade g where g.student.id=:s and g.gradeItem.teachingAssignment.schoolClass.id=:c and (g.gradeItem.assessmentDate is null or g.gradeItem.assessmentDate>=:d)",Long.class).setParameter("s",e.getStudent().getId()).setParameter("c",source.getId()).setParameter("d",r.effectiveDate()).getSingleResult();if(records>0||grades>0)conflict();
        em.createNativeQuery("update class_enrollments set left_at=:d,status='TRANSFERRED' where id=:id").setParameter("d",r.effectiveDate().minusDays(1)).setParameter("id",enrollmentId).executeUpdate();addEnrollment(a,target,e.getStudent(),r.effectiveDate());em.refresh(e);return map("sourceEnrollmentId",enrollmentId,"targetClassId",target.getId(),"studentId",e.getStudent().getId(),"effectiveDate",r.effectiveDate());}
    @Transactional public void endEnrollment(Long uid,Long id,EndEnrollment r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_ENDENROLLMENT","ClassEnrollment",id);ClassEnrollment e=enrollment(a,id);em.lock(e.getStudent(),LockModeType.PESSIMISTIC_WRITE);active(e.getSchoolClass());if(r.status()==EnrollmentStatus.ACTIVE||r.status()==EnrollmentStatus.TRANSFERRED||e.getStatus()!=EnrollmentStatus.ACTIVE||e.getLeftAt()!=null||r.leftAt().isBefore(e.getEnrolledAt().toLocalDate()))invalid();enrollmentDate(e.getSchoolClass(),r.leftAt());
        if(em.createQuery("select count(r) from AttendanceRecord r where r.student.id=:s and r.session.schoolClass.id=:c and r.session.sessionDate>:d",Long.class).setParameter("s",e.getStudent().getId()).setParameter("c",e.getSchoolClass().getId()).setParameter("d",r.leftAt()).getSingleResult()>0
            ||em.createQuery("select count(g) from StudentGrade g where g.student.id=:s and g.gradeItem.teachingAssignment.schoolClass.id=:c and (g.gradeItem.assessmentDate is null or g.gradeItem.assessmentDate>:d)",Long.class).setParameter("s",e.getStudent().getId()).setParameter("c",e.getSchoolClass().getId()).setParameter("d",r.leftAt()).getSingleResult()>0)conflict();
        em.createNativeQuery("update class_enrollments set left_at=:d,status=:status where id=:id").setParameter("d",r.leftAt()).setParameter("status",r.status().name()).setParameter("id",id).executeUpdate();em.refresh(e);}
    public Map<String,Object> roster(Long uid,Long classId,LocalDate date,int page,int size){User a=accounts.admin(uid);require(a,SchoolClass.class,classId);SchoolAdminAccountService.page(page,size);String where=" where e.schoolClass.id=:c and e.student.school.id=e.schoolClass.school.id"+(date==null?"":" and e.enrolledAt<:end and (e.leftAt is null or e.leftAt>=:start)");
        var query=em.createQuery("select e from ClassEnrollment e join fetch e.student s"+where+" order by s.fullName,e.id",ClassEnrollment.class);var count=em.createQuery("select count(e) from ClassEnrollment e"+where,Long.class);for(var q:List.of(query,count)){q.setParameter("c",classId);if(date!=null)q.setParameter("end",date.plusDays(1).atStartOfDay()).setParameter("start",date.atStartOfDay());}
        return map("content",query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(e->map("enrollmentId",e.getId(),"student",view(e.getStudent()),"enrolledAt",e.getEnrolledAt(),"leftAt",e.getLeftAt(),"status",e.getStatus())).toList(),"page",page,"size",size,"totalElements",count.getSingleResult());}
    private void activeTeacher(Teacher t){if(t.getStatus()!=TeacherStatus.ACTIVE||t.getUser().getStatus()!=UserStatus.ACTIVE||em.createQuery("select count(r) from UserRole r where r.user.id=:u and r.role.code='TEACHER'",Long.class).setParameter("u",t.getUser().getId()).getSingleResult()==0)invalid();}
    @Transactional public Map<String,Object> assignment(Long uid,Long id,Assignment r){validate(r);User a=writer(uid);audit.record(a,"ADMIN_ASSIGNMENT","TeachingAssignment",id);SchoolClass c=require(a,SchoolClass.class,r.classId());Teacher t=require(a,Teacher.class,r.teacherId());Subject sub=require(a,Subject.class,r.subjectId());Semester sem=require(a,Semester.class,r.semesterId());if(!sem.getAcademicYear().getId().equals(c.getAcademicYear().getId()))invalid();
        TeachingAssignment x=id==null?new TeachingAssignment():require(a,TeachingAssignment.class,id);
        if(id!=null&&(!x.getSchoolClass().getId().equals(r.classId())||!x.getTeacher().getId().equals(r.teacherId())||!x.getSubject().getId().equals(r.subjectId())||!x.getSemester().getId().equals(r.semesterId())))invalid();
        if(r.status()==ActiveStatus.ACTIVE){active(c);activeTeacher(t);if(!sub.isActive())invalid();}x.setSchoolClass(c);x.setTeacher(t);x.setSubject(sub);x.setSemester(sem);x.setStatus(r.status());if(id==null)em.persist(x);em.flush();return view(x);}
}
