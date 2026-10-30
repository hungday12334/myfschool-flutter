package com.example.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "timetable_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_timetable_student_date_period",
                columnNames = {"student_id", "lesson_date", "period_number"}
        ),
        indexes = @Index(
                name = "idx_timetable_student_date",
                columnList = "student_id,lesson_date"
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "subject_code", nullable = false, length = 40)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 120)
    private String subjectName;

    @Column(name = "teacher_name", nullable = false, length = 180)
    private String teacherName;

    @Column(nullable = false, length = 80)
    private String room;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
