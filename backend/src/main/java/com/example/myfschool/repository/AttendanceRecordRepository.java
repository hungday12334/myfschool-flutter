package com.example.myfschool.repository;

import com.example.myfschool.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByStudentId(Long studentId);

    List<AttendanceRecord> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long studentId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<AttendanceRecord> findByStudentIdInAndAttendanceDateOrderByStudentFullNameAsc(
            List<Long> studentIds, LocalDate date);

    Optional<AttendanceRecord> findByStudentIdAndAttendanceDate(Long studentId, LocalDate date);
}
