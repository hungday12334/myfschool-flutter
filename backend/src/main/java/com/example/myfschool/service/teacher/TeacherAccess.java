package com.example.myfschool.service.teacher;

import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Central ownership and tenant checks. User IDs are supplied only by authenticated controllers. */
@Service
@Transactional(readOnly = true)
public class TeacherAccess {
    private final EntityManager em;
    public TeacherAccess(EntityManager em) { this.em = em; }

    public Teacher teacher(Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        Teacher t = em.createQuery("select t from Teacher t join fetch t.user u join fetch t.school s where u.id=:id", Teacher.class)
                .setParameter("id",userId).getResultStream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        if (t.getStatus()!=TeacherStatus.ACTIVE || t.getUser().getStatus()!=UserStatus.ACTIVE
                || t.getSchool().getStatus()!=SchoolStatus.ACTIVE
                || !Objects.equals(t.getSchool().getId(), t.getUser().getSchool().getId())
                || em.createQuery("select count(r) from UserRole r where r.user.id=:id and r.role.code='TEACHER'",Long.class)
                    .setParameter("id",userId).getSingleResult()==0) deny();
        return t;
    }
    public SchoolClass classAccess(Teacher t, Long classId) {
        SchoolClass c = require(SchoolClass.class,classId);
        sameSchool(t,c.getSchool().getId());
        sameSchool(t,c.getAcademicYear().getSchool().getId());
        if (!isHomeroom(t,c) && em.createQuery("select count(a) from TeachingAssignment a where a.teacher.id=:t and a.schoolClass.id=:c and a.status=:active and a.subject.school.id=:s and a.semester.academicYear.id=:y",Long.class)
                .setParameter("t",t.getId()).setParameter("c",c.getId()).setParameter("active",ActiveStatus.ACTIVE)
                .setParameter("s",t.getSchool().getId()).setParameter("y",c.getAcademicYear().getId()).getSingleResult()==0) deny();
        return c;
    }
    public boolean isHomeroom(Teacher t, SchoolClass c) {
        return c.getHomeroomTeacher()!=null && Objects.equals(c.getHomeroomTeacher().getId(),t.getId());
    }
    public SchoolClass homeroom(Teacher t, Long classId) {
        if(classId==null) classId=em.createQuery("select c.id from SchoolClass c where c.homeroomTeacher.id=:t and c.school.id=:s and c.academicYear.school.id=:s and c.status=:active order by c.academicYear.isCurrent desc,c.academicYear.startDate desc,c.id",Long.class)
                .setParameter("t",t.getId()).setParameter("s",t.getSchool().getId()).setParameter("active",SchoolClassStatus.ACTIVE).setMaxResults(1).getResultStream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.ACCESS_DENIED));
        SchoolClass c=classAccess(t,classId);
        if (!isHomeroom(t,c)) deny();
        return c;
    }
    public TeachingAssignment assignment(Teacher t, Long assignmentId) {
        TeachingAssignment a=require(TeachingAssignment.class,assignmentId);
        sameSchool(t,a.getSchoolClass().getSchool().getId());
        sameSchool(t,a.getSubject().getSchool().getId());
        sameSchool(t,a.getSemester().getAcademicYear().getSchool().getId());
        if (!Objects.equals(a.getTeacher().getId(),t.getId()) || a.getStatus()!=ActiveStatus.ACTIVE
                || a.getSchoolClass().getStatus()!=SchoolClassStatus.ACTIVE
                || !Objects.equals(a.getSchoolClass().getAcademicYear().getId(),a.getSemester().getAcademicYear().getId())) deny();
        return a;
    }
    public Semester semester(Teacher t, Long id) {
        Semester s=require(Semester.class,id);
        sameSchool(t,s.getAcademicYear().getSchool().getId());
        return s;
    }
    public AcademicYear year(Teacher t, Long id) {
        AcademicYear y=require(AcademicYear.class,id); sameSchool(t,y.getSchool().getId()); return y;
    }
    public Student enrolled(Teacher t, SchoolClass c, Long studentId, LocalDate date) {
        sameSchool(t,c.getSchool().getId());
        Student s=require(Student.class,studentId); sameSchool(t,s.getSchool().getId());
        LocalDate day=date==null?LocalDate.now():date;
        if (day.isBefore(c.getAcademicYear().getStartDate()) || day.isAfter(c.getAcademicYear().getEndDate()))
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"Ngày không thuộc năm học của lớp");
        long count=em.createQuery("select count(e) from ClassEnrollment e where e.schoolClass.id=:c and e.student.id=:s and e.enrolledAt<:end and (e.leftAt is null or e.leftAt>=:start) and (e.status=:active or e.leftAt is not null)",Long.class)
                .setParameter("c",c.getId()).setParameter("s",studentId).setParameter("end",day.plusDays(1).atStartOfDay())
                .setParameter("start",day.atStartOfDay()).setParameter("active",EnrollmentStatus.ACTIVE).getSingleResult();
        if(count==0) deny(); return s;
    }
    public void sameSchool(Teacher t, Long schoolId) { if(!Objects.equals(t.getSchool().getId(),schoolId)) deny(); }
    public <T> T require(Class<T> type, Long id) {
        T value=id==null?null:em.find(type,id);
        if(value==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); return value;
    }
    public static void page(int page,int size) {
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"Phân trang không hợp lệ");
    }
    private void deny() { throw new BusinessException(ErrorCode.ACCESS_DENIED); }
}
