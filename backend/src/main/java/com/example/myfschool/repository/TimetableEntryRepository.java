package com.example.myfschool.repository;

import com.example.myfschool.entity.TimetableEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    @Query("""
            SELECT entry
            FROM TimetableEntry entry
            WHERE entry.student.id = :studentId
              AND entry.lessonDate BETWEEN :startDate AND :endDate
            ORDER BY entry.lessonDate ASC, entry.startTime ASC, entry.periodNumber ASC
            """)
    List<TimetableEntry> findWeeklyEntries(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    boolean existsByStudentIdAndLessonDateAndPeriodNumber(
            Long studentId,
            LocalDate lessonDate,
            int periodNumber
    );
}
