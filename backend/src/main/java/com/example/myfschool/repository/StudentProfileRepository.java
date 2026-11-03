package com.example.myfschool.repository;

import com.example.myfschool.entity.StudentProfile;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findFirstByParentIdOrderByIdAsc(Long parentId);

    Optional<StudentProfile> findByParentIdAndStudentCode(Long parentId, String studentCode);

    List<StudentProfile> findByClassNameIgnoreCaseAndSchoolNameIgnoreCaseOrderByFullNameAsc(
            String className, String schoolName);

    Optional<StudentProfile> findByIdAndClassNameIgnoreCaseAndSchoolNameIgnoreCase(
            Long id, String className, String schoolName);

    boolean existsByClassNameIgnoreCaseAndSchoolNameIgnoreCase(String className, String schoolName);
}
