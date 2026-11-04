package com.example.myfschool.repository;

import com.example.myfschool.entity.DeviceToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);
    Optional<DeviceToken> findByTokenAndUser_Id(String token, Long userId);
}
