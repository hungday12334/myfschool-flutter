package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="student_evaluations", uniqueConstraints={@UniqueConstraint(name="uk_evaluation_student_semester", columnNames={"student_id", "semester_id"})}, indexes={@Index(name="idx_evaluation_class", columnList="class_id"), @Index(name="idx_evaluation_semester", columnList="semester_id"), @Index(name="idx_evaluation_author", columnList="evaluated_by")})
@Getter
@Setter
@NoArgsConstructor
public class StudentEvaluation extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="semester_id", nullable=false)
    private Semester semester;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="evaluated_by")
    private User evaluatedBy;
    @Column(name="academic_result", length=50)
    private String academicResult;
    @Column(name="conduct_result", length=50)
    private String conductResult;
    @Column(name="overall_comment", columnDefinition="TEXT")
    private String overallComment;
    @Column(name="evaluated_at", insertable=false, updatable=false)
    private java.time.LocalDateTime evaluatedAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
