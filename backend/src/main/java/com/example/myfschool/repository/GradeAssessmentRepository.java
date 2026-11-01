package com.example.myfschool.repository;

import com.example.myfschool.entity.GradeAssessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeAssessmentRepository extends JpaRepository<GradeAssessment, Long> {

    boolean existsByStudentId(Long studentId);

    List<GradeAssessment> findByStudentIdOrderBySubjectNameAscSemesterAscIdAsc(Long studentId);

    List<GradeAssessment> findByStudentIdAndSemesterOrderBySubjectNameAscIdAsc(
            Long studentId,
            int semester
    );

    List<GradeAssessment> findByStudentIdAndSubjectCodeIgnoreCaseOrderBySemesterAscIdAsc(
            Long studentId,
            String subjectCode
    );

    List<GradeAssessment> findByStudentIdAndSubjectCodeIgnoreCaseAndSemesterOrderByIdAsc(
            Long studentId,
            String subjectCode,
            int semester
    );
}
