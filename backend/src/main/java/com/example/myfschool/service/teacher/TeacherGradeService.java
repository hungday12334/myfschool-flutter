package com.example.myfschool.service.teacher;

import static com.example.myfschool.dto.teacher.TeacherGradeDtos.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.ActiveStatus;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class TeacherGradeService {
    private final EntityManager em;
    private final TeacherAccess access;

    public TeacherGradeService(EntityManager em, TeacherAccess access) {
        this.em = em;
        this.access = access;
    }

    @Transactional
    public GradeItemResponse createItem(Long userId, @Valid CreateGradeItem request) {
        Teacher teacher = access.teacher(userId);
        TeachingAssignment assignment = access.assignment(teacher, request.teachingAssignmentId());
        validateDate(assignment.getSemester(), request.assessmentDate());
        GradeItem item = new GradeItem();
        item.setTeachingAssignment(assignment);
        item.setTitle(request.title().strip());
        item.setGradeType(request.gradeType());
        item.setWeight(request.weight());
        item.setMaxScore(request.maxScore());
        item.setAssessmentDate(request.assessmentDate());
        em.persist(item);
        return itemDto(item);
    }

    @Transactional
    public List<GradeResponse> enterGrades(Long userId, @Valid EnterGrades request) {
        Teacher teacher = access.teacher(userId);
        GradeItem item = requireItem(teacher, request.gradeItemId(), true);
        Set<Long> ids = new HashSet<>();
        for (GradeEntry entry : request.grades()) {
            if (!ids.add(entry.studentId())) conflict("A student occurs more than once in this batch");
            validateScore(entry.score(), item);
            access.enrolled(teacher, item.getTeachingAssignment().getSchoolClass(), entry.studentId(), enrollmentDate(item));
        }
        // Locking the common grade item serializes all creates for this item, including empty rows.
        Long existing = em.createQuery("select count(g.id) from StudentGrade g where g.gradeItem.id=:item and g.student.id in :students", Long.class)
                .setParameter("item", item.getId()).setParameter("students", ids).getSingleResult();
        if (existing != 0) conflict("A grade already exists; use PUT to edit it");
        List<GradeResponse> result = new ArrayList<>();
        for (GradeEntry entry : request.grades()) {
            StudentGrade grade = new StudentGrade();
            grade.setGradeItem(item);
            grade.setStudent(em.getReference(Student.class, entry.studentId()));
            grade.setScore(entry.score());
            grade.setComment(entry.comment());
            grade.setGradedBy(teacher.getUser());
            grade.setGradedAt(LocalDateTime.now());
            em.persist(grade);
            result.add(gradeDto(grade));
        }
        em.flush();
        return result;
    }

    @Transactional
    public GradeResponse editGrade(Long userId, Long id, @Valid EditGrade request) {
        Teacher teacher = access.teacher(userId);
        StudentGrade grade = em.find(StudentGrade.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (grade == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        GradeItem item = requireItem(teacher, grade.getGradeItem().getId(), false);
        access.enrolled(teacher, item.getTeachingAssignment().getSchoolClass(), grade.getStudent().getId(), enrollmentDate(item));
        validateScore(request.score(), item);
        grade.setScore(request.score());
        grade.setComment(request.comment());
        grade.setGradedBy(teacher.getUser());
        grade.setGradedAt(LocalDateTime.now());
        return gradeDto(grade);
    }

    public GradebookResponse gradebook(Long userId, Long classId, Long subjectId, Long semesterId) {
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass = access.classAccess(teacher, classId);
        Semester semester = resolveSemester(teacher, schoolClass, semesterId);
        List<TeachingAssignment> assignments = em.createQuery("select a from TeachingAssignment a join fetch a.subject where a.teacher.id=:teacher and a.schoolClass.id=:class and a.subject.id=:subject and a.semester.id=:semester and a.status=:active", TeachingAssignment.class)
                .setParameter("teacher", teacher.getId()).setParameter("class", classId).setParameter("subject", subjectId)
                .setParameter("semester", semester.getId()).setParameter("active", ActiveStatus.ACTIVE).getResultList();
        if (assignments.isEmpty()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        TeachingAssignment assignment = access.assignment(teacher, assignments.getFirst().getId());
        List<GradeItem> items = em.createQuery("select i from GradeItem i where i.teachingAssignment.id=:assignment order by i.assessmentDate,i.id", GradeItem.class)
                .setParameter("assignment", assignment.getId()).getResultList();
        List<Student> students = roster(schoolClass, semester, 0, Integer.MAX_VALUE);
        Map<Long, List<StudentGrade>> grades = items.isEmpty() ? Map.of() : em.createQuery("select g from StudentGrade g join fetch g.student where g.gradeItem.id in :items and g.student.school.id=:school order by g.gradeItem.id", StudentGrade.class)
                .setParameter("items", items.stream().map(GradeItem::getId).toList()).setParameter("school", teacher.getSchool().getId())
                .getResultList().stream().collect(Collectors.groupingBy(g -> g.getStudent().getId()));
        return new GradebookResponse(classId, schoolClass.getClassName(), named(assignment.getSubject()), named(semester),
                items.stream().map(this::itemDto).toList(), students.stream().map(s -> new GradebookStudent(s.getId(), s.getStudentCode(), s.getFullName(),
                        grades.getOrDefault(s.getId(), List.of()).stream().map(this::gradeDto).toList())).toList());
    }

    @Transactional
    public EvaluationResponse evaluate(Long userId, @Valid EvaluationRequest request) {
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass = access.homeroom(teacher, request.classId());
        Semester semester = resolveSemester(teacher, schoolClass, request.semesterId());
        Student student = enrolledDuringSemester(teacher, schoolClass, request.studentId(), semester);
        if (blank(request.academicResult()) && blank(request.conductResult()) && blank(request.overallComment()))
            invalid("Provide an academic result, conduct result or overall comment");
        em.lock(student, LockModeType.PESSIMISTIC_WRITE);
        if (em.createQuery("select count(e.id) from StudentEvaluation e where e.student.id=:student and e.semester.id=:semester", Long.class)
                .setParameter("student", student.getId()).setParameter("semester", semester.getId()).getSingleResult() > 0)
            conflict("An evaluation already exists for this student and semester");
        StudentEvaluation evaluation = new StudentEvaluation();
        evaluation.setStudent(student);
        evaluation.setSchoolClass(schoolClass);
        evaluation.setSemester(semester);
        evaluation.setAcademicResult(request.academicResult());
        evaluation.setConductResult(request.conductResult());
        evaluation.setOverallComment(request.overallComment());
        evaluation.setEvaluatedBy(teacher.getUser());
        evaluation.setEvaluatedAt(LocalDateTime.now());
        em.persist(evaluation);
        return evaluationDto(evaluation);
    }

    @Transactional
    public RemarkResponse remark(Long userId, @Valid RemarkRequest request) {
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass;
        if (request.teachingAssignmentId() == null) {
            schoolClass = access.homeroom(teacher, request.classId());
        } else {
            TeachingAssignment assignment = access.assignment(teacher, request.teachingAssignmentId());
            schoolClass = assignment.getSchoolClass();
            if (!schoolClass.getId().equals(request.classId()) || !assignment.getSemester().getId().equals(request.semesterId()))
                invalid("Teaching assignment does not match class and semester");
        }
        Semester semester = resolveSemester(teacher, schoolClass, request.semesterId());
        validateDate(semester, request.remarkDate());
        Student student = access.enrolled(teacher, schoolClass, request.studentId(), request.remarkDate());
        StudentRemark remark = new StudentRemark();
        remark.setStudent(student);
        remark.setTeacher(teacher);
        remark.setSchoolClass(schoolClass);
        remark.setSemester(semester);
        remark.setContent(request.content().strip());
        remark.setRemarkDate(request.remarkDate());
        remark.setVisibility(request.visibility());
        em.persist(remark);
        // Schema stores class/semester remarks, not subject IDs. Assignment is authorization input only.
        return new RemarkResponse(remark.getId(), student.getId(), schoolClass.getId(), semester.getId(), teacher.getId(),
                teacher.getFullName(), remark.getContent(), remark.getRemarkDate(), remark.getVisibility());
    }

    public AcademicSummary homeroomSummary(Long userId, Long classId, Long academicYearId, Long semesterId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) invalid("Invalid pagination");
        Teacher teacher = access.teacher(userId);
        SchoolClass schoolClass = access.homeroom(teacher, classId);
        if (academicYearId != null && !academicYearId.equals(schoolClass.getAcademicYear().getId())) invalid("Academic year does not match homeroom class");
        Semester semester = resolveSemester(teacher, schoolClass, semesterId);
        List<Student> students = roster(schoolClass, semester, page * size, size);
        long total = em.createQuery("select count(distinct s.id) from ClassEnrollment e join e.student s where " + rosterWhere(), Long.class)
                .setParameter("class", schoolClass.getId()).setParameter("school", schoolClass.getSchool().getId())
                .setParameter("end", semester.getEndDate().plusDays(1).atStartOfDay()).setParameter("start", semester.getStartDate().atStartOfDay()).getSingleResult();
        List<Long> ids = students.stream().map(Student::getId).toList();
        List<GradeItem> items = em.createQuery("select i from GradeItem i join fetch i.teachingAssignment a join fetch a.subject s join fetch a.semester where a.schoolClass.id=:class and a.semester.id=:semester and s.school.id=:school order by s.name,i.assessmentDate,i.id", GradeItem.class)
                .setParameter("class", schoolClass.getId()).setParameter("semester", semester.getId()).setParameter("school", teacher.getSchool().getId()).getResultList();
        Map<Long,List<GradeItem>> subjects = items.stream().collect(Collectors.groupingBy(i -> i.getTeachingAssignment().getSubject().getId(), LinkedHashMap::new, Collectors.toList()));
        List<StudentGrade> grades = ids.isEmpty() || items.isEmpty() ? List.of() : em.createQuery("select g from StudentGrade g join fetch g.gradeItem where g.student.id in :students and g.gradeItem.id in :items", StudentGrade.class)
                .setParameter("students", ids).setParameter("items", items.stream().map(GradeItem::getId).toList()).getResultList();
        Map<Long,List<StudentGrade>> byStudent = grades.stream().collect(Collectors.groupingBy(g -> g.getStudent().getId()));
        Map<Long,StudentEvaluation> evaluations = ids.isEmpty() ? Map.of() : em.createQuery("select e from StudentEvaluation e left join fetch e.evaluatedBy where e.student.id in :students and e.semester.id=:semester and e.schoolClass.id=:class", StudentEvaluation.class)
                .setParameter("students", ids).setParameter("semester", semester.getId()).setParameter("class", schoolClass.getId()).getResultList().stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e));
        List<StudentAcademicSummary> content = students.stream().map(s -> {
            List<StudentGrade> studentGrades = byStudent.getOrDefault(s.getId(), List.of());
            List<SubjectResult> results = subjects.values().stream().map(subjectItems -> {
                Subject subject = subjectItems.getFirst().getTeachingAssignment().getSubject();
                Set<Long> itemIds = subjectItems.stream().map(GradeItem::getId).collect(Collectors.toSet());
                return new SubjectResult(named(subject), subjectItems.stream().map(this::itemDto).toList(),
                        studentGrades.stream().filter(g -> itemIds.contains(g.getGradeItem().getId())).map(this::gradeDto).toList());
            }).toList();
            return new StudentAcademicSummary(s.getId(), s.getStudentCode(), s.getFullName(), results,
                    evaluations.containsKey(s.getId()) ? evaluationDto(evaluations.get(s.getId())) : null);
        }).toList();
        return new AcademicSummary(schoolClass.getId(), schoolClass.getClassName(), named(semester), content, page, size, total, (total + size - 1) / size);
    }

    private GradeItem requireItem(Teacher teacher, Long id, boolean lock) {
        GradeItem item = lock ? em.find(GradeItem.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(GradeItem.class, id);
        if (item == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        access.assignment(teacher, item.getTeachingAssignment().getId());
        return item;
    }

    private Semester resolveSemester(Teacher teacher, SchoolClass schoolClass, Long semesterId) {
        Semester semester;
        if (semesterId == null) {
            var candidates = em.createQuery("select s from Semester s where s.academicYear.id=:year order by s.semesterNumber desc", Semester.class)
                    .setParameter("year", schoolClass.getAcademicYear().getId()).getResultList();
            if (candidates.isEmpty()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "No semester configured for this class");
            LocalDate today = LocalDate.now();
            semester = candidates.stream().filter(s -> !today.isBefore(s.getStartDate()) && !today.isAfter(s.getEndDate())).findFirst().orElse(candidates.getFirst());
        } else semester = access.semester(teacher, semesterId);
        if (!semester.getAcademicYear().getId().equals(schoolClass.getAcademicYear().getId())) invalid("Semester does not belong to the class academic year");
        return semester;
    }

    private Student enrolledDuringSemester(Teacher teacher, SchoolClass schoolClass, Long studentId, Semester semester) {
        List<ClassEnrollment> enrollments = em.createQuery("select e from ClassEnrollment e join fetch e.student s where " + rosterWhere() + " and s.id=:student", ClassEnrollment.class)
                .setParameter("class", schoolClass.getId()).setParameter("school", teacher.getSchool().getId()).setParameter("student", studentId)
                .setParameter("end", semester.getEndDate().plusDays(1).atStartOfDay()).setParameter("start", semester.getStartDate().atStartOfDay()).getResultList();
        if (enrollments.isEmpty()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return enrollments.getFirst().getStudent();
    }

    private List<Student> roster(SchoolClass schoolClass, Semester semester, int offset, int limit) {
        return em.createQuery("select distinct s from ClassEnrollment e join e.student s where " + rosterWhere() + " order by s.fullName,s.id", Student.class)
                .setParameter("class", schoolClass.getId()).setParameter("school", schoolClass.getSchool().getId())
                .setParameter("end", semester.getEndDate().plusDays(1).atStartOfDay()).setParameter("start", semester.getStartDate().atStartOfDay())
                .setFirstResult(offset).setMaxResults(limit).getResultList();
    }
    private String rosterWhere() { return "e.schoolClass.id=:class and s.school.id=:school and e.enrolledAt<:end and (e.leftAt is null or e.leftAt>=:start) and (e.status=com.example.myfschool.enums.EnrollmentStatus.ACTIVE or e.leftAt is not null)"; }
    private LocalDate enrollmentDate(GradeItem item) {
        if (item.getAssessmentDate() != null) return item.getAssessmentDate();
        Semester semester = item.getTeachingAssignment().getSemester();
        LocalDate today = LocalDate.now();
        return today.isBefore(semester.getStartDate()) ? semester.getStartDate() : today.isAfter(semester.getEndDate()) ? semester.getEndDate() : today;
    }
    private void validateDate(Semester semester, LocalDate date) {
        if (date != null && (date.isBefore(semester.getStartDate()) || date.isAfter(semester.getEndDate()))) invalid("Date must be within the assignment semester");
    }
    private void validateScore(BigDecimal score, GradeItem item) {
        if (score != null && (score.signum() < 0 || score.compareTo(item.getMaxScore()) > 0 || score.scale() > 2)) invalid("Score must be between zero and the grade item's maximum, with at most two decimals");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private static void invalid(String message) { throw new BusinessException(ErrorCode.INVALID_REQUEST, message); }
    private static void conflict(String message) { throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, message); }
    private NamedId named(Subject subject) { return new NamedId(subject.getId(), subject.getName()); }
    private NamedId named(Semester semester) { return new NamedId(semester.getId(), semester.getName()); }
    private GradeItemResponse itemDto(GradeItem item) {
        TeachingAssignment a = item.getTeachingAssignment();
        return new GradeItemResponse(item.getId(), a.getId(), a.getSchoolClass().getId(), named(a.getSubject()), named(a.getSemester()), item.getTitle(), item.getGradeType(), item.getWeight(), item.getMaxScore(), item.getAssessmentDate());
    }
    private GradeResponse gradeDto(StudentGrade grade) { return new GradeResponse(grade.getId(), grade.getGradeItem().getId(), grade.getStudent().getId(), grade.getScore(), grade.getComment(), grade.getGradedAt()); }
    private EvaluationResponse evaluationDto(StudentEvaluation evaluation) {
        return new EvaluationResponse(evaluation.getId(), evaluation.getStudent().getId(), evaluation.getSchoolClass().getId(), evaluation.getSemester().getId(), evaluation.getAcademicResult(), evaluation.getConductResult(), evaluation.getOverallComment(), evaluation.getEvaluatedBy() == null ? null : evaluation.getEvaluatedBy().getId(), evaluation.getEvaluatedAt());
    }
}
