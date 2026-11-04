package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="school_classes", uniqueConstraints={@UniqueConstraint(name="uk_class_year_name", columnNames={"academic_year_id", "class_name"})}, indexes={@Index(name="idx_class_school", columnList="school_id"), @Index(name="idx_class_homeroom", columnList="homeroom_teacher_id")})
@Getter
@Setter
@NoArgsConstructor
public class SchoolClass extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="academic_year_id", nullable=false)
    private AcademicYear academicYear;
    @Column(name="class_name", nullable=false, length=50)
    private String className;
    @Column(name="grade_level", nullable=false)
    private int gradeLevel;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="homeroom_teacher_id")
    private Teacher homeroomTeacher;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private SchoolClassStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
