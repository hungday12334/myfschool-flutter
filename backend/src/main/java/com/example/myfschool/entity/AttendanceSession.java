package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="attendance_sessions", indexes={@Index(name="idx_session_class_date", columnList="class_id,session_date"), @Index(name="idx_session_assignment", columnList="teaching_assignment_id"), @Index(name="idx_session_timetable", columnList="timetable_entry_id"), @Index(name="idx_session_creator", columnList="created_by")})
@Getter
@Setter
@NoArgsConstructor
public class AttendanceSession extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="teaching_assignment_id")
    private TeachingAssignment teachingAssignment;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="timetable_entry_id")
    private TimetableEntry timetableEntry;
    @Column(name="session_date", nullable=false)
    private java.time.LocalDate sessionDate;
    @Enumerated(EnumType.STRING)
    @Column(name="shift", nullable=false)
    private Shift shift;
    @Column(name="period_number")
    private Integer periodNumber;
    @Enumerated(EnumType.STRING)
    @Column(name="session_type", nullable=false)
    private AttendanceSessionType sessionType;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private AttendanceSessionStatus status;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="created_by", nullable=false)
    private User createdBy;
    @Column(name="completed_at", insertable=false, updatable=false)
    private java.time.LocalDateTime completedAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
