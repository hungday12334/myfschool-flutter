package com.example.myfschool.repository;

import com.example.myfschool.entity.School;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findBySchoolCode(String schoolCode);
}
