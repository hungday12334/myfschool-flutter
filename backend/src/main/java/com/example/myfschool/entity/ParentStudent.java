package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="parent_students", uniqueConstraints={@UniqueConstraint(name="uk_parent_student", columnNames={"parent_id", "student_id"})}, indexes={@Index(name="idx_parent_student_student", columnList="student_id")})
@Getter
@Setter
@NoArgsConstructor
public class ParentStudent extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="parent_id", nullable=false)
    private Parent parent;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @Enumerated(EnumType.STRING)
    @Column(name="relationship_type", nullable=false)
    private RelationshipType relationshipType;
    @Column(name="is_primary_contact", nullable=false)
    private boolean isPrimaryContact;
    @Column(name="can_view_academic", nullable=false)
    private boolean canViewAcademic;
    @Column(name="can_submit_leave", nullable=false)
    private boolean canSubmitLeave;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
}
