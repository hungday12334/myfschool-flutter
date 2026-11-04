package com.example.myfschool.repository;

import com.example.myfschool.entity.LeaveRequest;
import com.example.myfschool.enums.LeaveRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Optional<LeaveRequest> findByIdAndParent_Id(Long id, Long parentId);
    Page<LeaveRequest> findByParent_IdAndStudent_Id(Long parentId, Long studentId, Pageable pageable);
    Page<LeaveRequest> findByParent_IdAndStudent_IdAndStatus(Long parentId, Long studentId, LeaveRequestStatus status, Pageable pageable);
    boolean existsByStudent_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long studentId, LeaveRequestStatus status, java.time.LocalDate endDate, java.time.LocalDate startDate);
    long countByParent_IdAndStatus(Long parentId, LeaveRequestStatus status);
}
