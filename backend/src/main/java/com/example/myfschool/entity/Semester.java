package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="semesters", uniqueConstraints={@UniqueConstraint(name="uk_semester_year_number", columnNames={"academic_year_id", "semester_number"})})
@Getter
@Setter
@NoArgsConstructor
public class Semester extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="academic_year_id", nullable=false)
    private AcademicYear academicYear;
    @Column(name="semester_number", nullable=false)
    private int semesterNumber;
    @Column(name="name", nullable=false, length=60)
    private String name;
    @Column(name="start_date", nullable=false)
    private java.time.LocalDate startDate;
    @Column(name="end_date", nullable=false)
    private java.time.LocalDate endDate;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
