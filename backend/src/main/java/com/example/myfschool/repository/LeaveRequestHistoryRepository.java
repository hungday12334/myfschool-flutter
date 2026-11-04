package com.example.myfschool.repository;

import com.example.myfschool.entity.LeaveRequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestHistoryRepository extends JpaRepository<LeaveRequestHistory, Long> {
    List<LeaveRequestHistory> findByLeaveRequest_IdOrderByChangedAtAsc(Long requestId);
    List<LeaveRequestHistory> findByLeaveRequest_IdInOrderByChangedAtAsc(List<Long> requestIds);
}
