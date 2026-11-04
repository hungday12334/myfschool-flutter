package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="teaching_assignments", uniqueConstraints={@UniqueConstraint(name="uk_assignment", columnNames={"class_id", "teacher_id", "subject_id", "semester_id"})}, indexes={@Index(name="idx_assignment_teacher_semester", columnList="teacher_id,semester_id"), @Index(name="idx_assignment_subject", columnList="subject_id"), @Index(name="idx_assignment_semester", columnList="semester_id")})
@Getter
@Setter
@NoArgsConstructor
public class TeachingAssignment extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="teacher_id", nullable=false)
    private Teacher teacher;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="subject_id", nullable=false)
    private Subject subject;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="semester_id", nullable=false)
    private Semester semester;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private ActiveStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
