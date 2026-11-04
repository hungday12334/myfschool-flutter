package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="student_grades", uniqueConstraints={@UniqueConstraint(name="uk_student_grade", columnNames={"grade_item_id", "student_id"})}, indexes={@Index(name="idx_grade_student", columnList="student_id"), @Index(name="idx_grade_marker", columnList="graded_by")})
@Getter
@Setter
@NoArgsConstructor
public class StudentGrade extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="grade_item_id", nullable=false)
    private GradeItem gradeItem;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @Column(name="score", precision=6, scale=2)
    private java.math.BigDecimal score;
    @Column(name="comment", columnDefinition="TEXT")
    private String comment;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="graded_by")
    private User gradedBy;
    @Column(name="graded_at", insertable=false, updatable=false)
    private java.time.LocalDateTime gradedAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
