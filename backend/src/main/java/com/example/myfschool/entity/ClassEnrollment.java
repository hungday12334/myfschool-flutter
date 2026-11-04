package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="class_enrollments", uniqueConstraints={@UniqueConstraint(name="uk_enrollment_student_class_start", columnNames={"student_id", "class_id", "enrolled_at"})}, indexes={@Index(name="idx_enrollment_class_status", columnList="class_id,status"), @Index(name="idx_enrollment_student_status", columnList="student_id,status")})
@Getter
@Setter
@NoArgsConstructor
public class ClassEnrollment extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @Column(name="enrolled_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime enrolledAt;
    @Column(name="left_at", insertable=false, updatable=false)
    private java.time.LocalDateTime leftAt;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private EnrollmentStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
