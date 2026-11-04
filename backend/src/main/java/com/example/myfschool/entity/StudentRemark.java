package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="student_remarks", indexes={@Index(name="idx_remark_student_date", columnList="student_id,remark_date"), @Index(name="idx_remark_teacher", columnList="teacher_id"), @Index(name="idx_remark_class", columnList="class_id"), @Index(name="idx_remark_semester", columnList="semester_id")})
@Getter
@Setter
@NoArgsConstructor
public class StudentRemark extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="teacher_id", nullable=false)
    private Teacher teacher;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="class_id", nullable=false)
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="semester_id", nullable=false)
    private Semester semester;
    @Column(name="content", nullable=false, columnDefinition="TEXT")
    private String content;
    @Column(name="remark_date", nullable=false)
    private java.time.LocalDate remarkDate;
    @Enumerated(EnumType.STRING)
    @Column(name="visibility", nullable=false)
    private RemarkVisibility visibility;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
