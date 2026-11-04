package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="teachers", uniqueConstraints={@UniqueConstraint(name="uk_teacher_user", columnNames={"user_id"}), @UniqueConstraint(name="uk_teacher_code", columnNames={"school_id", "teacher_code"})})
@Getter
@Setter
@NoArgsConstructor
public class Teacher extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="teacher_code", nullable=false, length=40)
    private String teacherCode;
    @Column(name="full_name", nullable=false, length=150)
    private String fullName;
    @Column(name="date_of_birth")
    private java.time.LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    @Column(name="gender")
    private Gender gender;
    @Column(name="phone", length=20)
    private String phone;
    @Column(name="email", length=255)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private TeacherStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
