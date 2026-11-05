package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="attendance_records", uniqueConstraints={@UniqueConstraint(name="uk_attendance_session_student", columnNames={"session_id", "student_id"})}, indexes={@Index(name="idx_attendance_student", columnList="student_id"), @Index(name="idx_attendance_marked_by", columnList="marked_by")})
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="session_id", nullable=false)
    private AttendanceSession session;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private AttendanceStatus status;
    @Column(name="note", length=500)
    private String note;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="marked_by")
    private User markedBy;
    @Column(name="marked_at", nullable=false)
    private java.time.LocalDateTime markedAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
