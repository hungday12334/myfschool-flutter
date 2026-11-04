package com.example.myfschool.repository;

import com.example.myfschool.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Long> {
    boolean existsByUser_Id(Long userId);
}
