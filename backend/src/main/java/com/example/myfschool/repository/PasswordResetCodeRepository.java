package com.example.myfschool.repository;

import com.example.myfschool.entity.PasswordResetCode;
import com.example.myfschool.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findFirstByUserOrderByCreatedAtDesc(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetCode> findFirstByUserAndConsumedAtIsNullOrderByCreatedAtDesc(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetCode> findByResetTokenHashAndConsumedAtIsNull(String resetTokenHash);

    List<PasswordResetCode> findAllByUserAndConsumedAtIsNull(User user);
}
