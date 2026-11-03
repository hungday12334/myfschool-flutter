package com.example.myfschool.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_class_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_teacher_class_school",
                columnNames = {"teacher_id", "class_name", "school_name"}),
        indexes = @Index(name = "idx_teacher_assignment_teacher", columnList = "teacher_id"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeacherClassAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "class_name", nullable = false, length = 60)
    private String className;

    @Column(name = "school_name", nullable = false, length = 220)
    private String schoolName;
}
