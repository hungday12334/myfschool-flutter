package com.example.myfschool.repository;

import com.example.myfschool.entity.Parent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Long> {
    boolean existsByUser_Id(Long userId);
    Optional<Parent> findByUser_Id(Long userId);
}
