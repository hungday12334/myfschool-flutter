package com.example.myfschool.repository;

import com.example.myfschool.entity.TeacherClassAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherClassAssignmentRepository extends JpaRepository<TeacherClassAssignment, Long> {
    List<TeacherClassAssignment> findByTeacherIdOrderByClassNameAsc(Long teacherId);
    Optional<TeacherClassAssignment> findByIdAndTeacherId(Long id, Long teacherId);
    boolean existsByTeacherIdAndClassNameIgnoreCaseAndSchoolNameIgnoreCase(
            Long teacherId, String className, String schoolName);
}
