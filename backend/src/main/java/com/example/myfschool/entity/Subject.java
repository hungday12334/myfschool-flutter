package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="subjects", uniqueConstraints={@UniqueConstraint(name="uk_subject_school_code", columnNames={"school_id", "subject_code"})})
@Getter
@Setter
@NoArgsConstructor
public class Subject extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @Column(name="subject_code", nullable=false, length=40)
    private String subjectCode;
    @Column(name="name", nullable=false, length=150)
    private String name;
    @Column(name="description", columnDefinition="TEXT")
    private String description;
    @Column(name="is_active", nullable=false)
    private boolean isActive;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
