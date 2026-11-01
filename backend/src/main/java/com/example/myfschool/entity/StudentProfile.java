package com.example.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_profile_parent_code",
                columnNames = {"parent_id", "student_code"}
        ),
        indexes = @Index(name = "idx_student_profile_parent", columnList = "parent_id")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "class_name", nullable = false, length = 60)
    private String className;

    @Column(name = "school_name", nullable = false, length = 220)
    private String schoolName;
}
