package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="grade_items", indexes={@Index(name="idx_grade_item_assignment", columnList="teaching_assignment_id")})
@Getter
@Setter
@NoArgsConstructor
public class GradeItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="teaching_assignment_id", nullable=false)
    private TeachingAssignment teachingAssignment;
    @Column(name="title", nullable=false, length=200)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name="grade_type", nullable=false)
    private GradeType gradeType;
    @Column(name="weight", nullable=false, precision=6, scale=2)
    private java.math.BigDecimal weight;
    @Column(name="max_score", nullable=false, precision=6, scale=2)
    private java.math.BigDecimal maxScore;
    @Column(name="assessment_date")
    private java.time.LocalDate assessmentDate;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
