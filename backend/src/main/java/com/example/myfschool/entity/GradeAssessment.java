package com.example.myfschool.entity;

import com.example.myfschool.enums.AssessmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "grade_assessments",
        indexes = {
                @Index(
                        name = "idx_grade_student_semester",
                        columnList = "student_id,semester"
                ),
                @Index(
                        name = "idx_grade_student_subject",
                        columnList = "student_id,subject_code"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GradeAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false)
    private int semester;

    @Column(name = "subject_code", nullable = false, length = 40)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 120)
    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 40)
    private AssessmentType assessmentType;

    @Column(name = "assessment_label", nullable = false, length = 80)
    private String assessmentLabel;

    @Column(nullable = false)
    private int coefficient;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal score;
}
