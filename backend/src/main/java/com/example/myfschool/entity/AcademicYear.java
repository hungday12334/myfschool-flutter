package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="academic_years", uniqueConstraints={@UniqueConstraint(name="uk_academic_year_school_name", columnNames={"school_id", "name"})})
@Getter
@Setter
@NoArgsConstructor
public class AcademicYear extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @Column(name="name", nullable=false, length=20)
    private String name;
    @Column(name="start_date", nullable=false)
    private java.time.LocalDate startDate;
    @Column(name="end_date", nullable=false)
    private java.time.LocalDate endDate;
    @Column(name="is_current", nullable=false)
    private boolean isCurrent;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
