package com.example.myfschool;

import com.example.myfschool.entity.*;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.*;
import com.example.myfschool.security.JwtService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Real database, persistence, security and MVC fixture. Every test rolls back its own data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("teacher-test")
@Transactional
public abstract class TeacherIntegrationSupport {
    @Autowired protected EntityManager em;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected MockMvc mvc;
    @Autowired protected JwtService jwt;
    protected School school, otherSchool;
    protected User teacherUser, subjectUser, otherUser, parentUser;
    protected Teacher teacher, subjectTeacher, otherTeacher;
    protected AcademicYear year;
    protected Semester semester;
    protected SchoolClass schoolClass, unrelatedClass;
    protected Subject subject;
    protected TeachingAssignment assignment, subjectAssignment;
    protected Student student, secondStudent, outsider;
    protected Parent parent;
    protected LocalDate today=LocalDate.now();
    private String suffix;

    @BeforeEach
    void setupTeacherFixture() {
        suffix=UUID.randomUUID().toString().substring(0,8);
        school=school("S"); otherSchool=school("X");
        teacherUser=user(school,"home","TEACHER"); subjectUser=user(school,"subject","TEACHER");
        otherUser=user(otherSchool,"other","TEACHER"); parentUser=user(school,"parent","PARENT");
        teacher=teacher(school,teacherUser,"T1"); subjectTeacher=teacher(school,subjectUser,"T2"); otherTeacher=teacher(otherSchool,otherUser,"T3");
        year=new AcademicYear(); year.setSchool(school);year.setName("Year-"+suffix);year.setStartDate(today.minusMonths(5));year.setEndDate(today.plusMonths(5));year.setCurrent(true);persist(year);
        semester=new Semester();semester.setAcademicYear(year);semester.setName("Học kỳ 1");semester.setSemesterNumber(1);semester.setStartDate(year.getStartDate());semester.setEndDate(year.getEndDate());persist(semester);
        schoolClass=clazz("10A",teacher); unrelatedClass=clazz("10B",null);
        subject=new Subject();subject.setSchool(school);subject.setSubjectCode("MATH"+suffix);subject.setName("Toán");subject.setActive(true);persist(subject);
        assignment=assignment(teacher); subjectAssignment=assignment(subjectTeacher);
        student=student(school,"A");secondStudent=student(school,"B");outsider=student(otherSchool,"X");
        enroll(student);enroll(secondStudent);
        parent=new Parent();parent.setSchool(school);parent.setUser(parentUser);parent.setFullName("Phụ huynh");persist(parent);
        ParentStudent link=new ParentStudent();link.setParent(parent);link.setStudent(student);link.setRelationshipType(RelationshipType.GUARDIAN);link.setPrimaryContact(true);link.setCanViewAcademic(true);link.setCanSubmitLeave(true);persist(link);
        em.flush();
    }
    protected <T> T persist(T value) { em.persist(value); return value; }
    protected String bearer(User u,String role) { return "Bearer "+jwt.generateToken(u,List.of(role)); }
    protected School school(String code) { School s=new School();s.setSchoolCode(code+suffix);s.setName(code);s.setSchoolLevel(SchoolLevel.COMBINED);s.setStatus(SchoolStatus.ACTIVE);return persist(s); }
    protected User user(School s,String name,String roleCode) {
        User u=new User();u.setSchool(s);u.setUsername(name+suffix);u.setPasswordHash("{test-only}");u.setStatus(UserStatus.ACTIVE);persist(u);
        Role r=em.createQuery("select r from Role r where r.code=:code",Role.class).setParameter("code",roleCode).getResultStream().findFirst().orElseGet(()->{Role n=new Role();n.setCode(roleCode);n.setName(roleCode);return persist(n);});
        UserRole ur=new UserRole();ur.setUser(u);ur.setRole(r);persist(ur);return u;
    }
    protected Teacher teacher(School s,User u,String code) {Teacher t=new Teacher();t.setSchool(s);t.setUser(u);t.setTeacherCode(code+suffix);t.setFullName(code);t.setStatus(TeacherStatus.ACTIVE);return persist(t);}
    protected SchoolClass clazz(String name,Teacher home) {SchoolClass c=new SchoolClass();c.setSchool(school);c.setAcademicYear(year);c.setClassName(name+suffix);c.setGradeLevel(10);c.setHomeroomTeacher(home);c.setStatus(SchoolClassStatus.ACTIVE);return persist(c);}
    protected TeachingAssignment assignment(Teacher t) {TeachingAssignment a=new TeachingAssignment();a.setSchoolClass(schoolClass);a.setTeacher(t);a.setSubject(subject);a.setSemester(semester);a.setStatus(ActiveStatus.ACTIVE);return persist(a);}
    protected Student student(School s,String name) {Student st=new Student();st.setSchool(s);st.setStudentCode(name+suffix);st.setFullName(name);st.setStatus(StudentStatus.ACTIVE);return persist(st);}
    protected void enroll(Student s) {ClassEnrollment e=new ClassEnrollment();e.setSchoolClass(schoolClass);e.setStudent(s);e.setStatus(EnrollmentStatus.ACTIVE);persist(e);em.flush();jdbc.update("update class_enrollments set enrolled_at=? where id=?",java.sql.Timestamp.valueOf(year.getStartDate().atStartOfDay()),e.getId());em.refresh(e);}
}
