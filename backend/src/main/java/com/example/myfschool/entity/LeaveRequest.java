package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="leave_requests", indexes={@Index(name="idx_leave_student_dates", columnList="student_id,start_date,end_date"), @Index(name="idx_leave_class_status", columnList="class_id,status"), @Index(name="idx_leave_parent", columnList="parent_id"), @Index(name="idx_leave_school", columnList="school_id"), @Index(name="idx_leave_reviewer", columnList="reviewed_by")})
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="parent_id", nullable=false)
    private Parent parent;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @Column(name="start_date", nullable=false)
    private java.time.LocalDate startDate;
    @Column(name="end_date", nullable=false)
    private java.time.LocalDate endDate;
    @Column(name="reason", nullable=false, columnDefinition="TEXT")
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private LeaveRequestStatus status;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="reviewed_by")
    private User reviewedBy;
    @Column(name="reviewed_at", insertable=false, updatable=false)
    private java.time.LocalDateTime reviewedAt;
    @Column(name="review_note", columnDefinition="TEXT")
    private String reviewNote;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
