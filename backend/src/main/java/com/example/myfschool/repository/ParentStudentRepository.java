package com.example.myfschool.repository;

import com.example.myfschool.entity.ParentStudent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {
    @EntityGraph(attributePaths = {"student", "student.school"})
    List<ParentStudent> findByParent_IdOrderByStudent_FullNameAsc(Long parentId);
    Optional<ParentStudent> findByParent_IdAndStudent_Id(Long parentId, Long studentId);
}
