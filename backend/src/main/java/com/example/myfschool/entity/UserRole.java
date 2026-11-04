package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="user_roles", uniqueConstraints={@UniqueConstraint(name="uk_user_role", columnNames={"user_id", "role_id"})}, indexes={@Index(name="idx_user_roles_role", columnList="role_id")})
@Getter
@Setter
@NoArgsConstructor
public class UserRole extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="role_id", nullable=false)
    private Role role;
    @Column(name="assigned_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime assignedAt;
}
