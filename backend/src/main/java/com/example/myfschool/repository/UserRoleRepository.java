package com.example.myfschool.repository;

import com.example.myfschool.entity.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @EntityGraph(attributePaths = "role")
    List<UserRole> findByUser_IdOrderByIdAsc(Long userId);
}
