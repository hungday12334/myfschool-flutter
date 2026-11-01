package com.example.myfschool.repository;

import com.example.myfschool.entity.StudentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findFirstByParentIdOrderByIdAsc(Long parentId);

    Optional<StudentProfile> findByParentIdAndStudentCode(Long parentId, String studentCode);
}
