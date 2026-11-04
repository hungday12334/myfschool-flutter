package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="students", uniqueConstraints={@UniqueConstraint(name="uk_student_user", columnNames={"user_id"}), @UniqueConstraint(name="uk_student_code", columnNames={"school_id", "student_code"})}, indexes={@Index(name="idx_student_school_status", columnList="school_id,status")})
@Getter
@Setter
@NoArgsConstructor
public class Student extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;
    @Column(name="student_code", nullable=false, length=40)
    private String studentCode;
    @Column(name="full_name", nullable=false, length=150)
    private String fullName;
    @Column(name="date_of_birth")
    private java.time.LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    @Column(name="gender")
    private Gender gender;
    @Column(name="admission_date")
    private java.time.LocalDate admissionDate;
    @Column(name="address", length=500)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private StudentStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
