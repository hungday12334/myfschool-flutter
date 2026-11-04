package com.example.myfschool.repository;

import com.example.myfschool.entity.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser_IdOrderByIdAsc(Long userId);
}
