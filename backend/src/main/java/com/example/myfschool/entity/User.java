package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="users", uniqueConstraints={@UniqueConstraint(name="uk_user_username", columnNames={"username"})}, indexes={@Index(name="idx_user_school_status", columnList="school_id,status")})
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @Column(name="username", nullable=false, length=100)
    private String username;
    @Column(name="password_hash", nullable=false, length=255)
    private String passwordHash;
    @Column(name="phone", length=20)
    private String phone;
    @Column(name="email", length=255)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private UserStatus status;
    @Column(name="must_change_password", nullable=false)
    private boolean mustChangePassword;
    @Column(name="last_login_at", insertable=false, updatable=false)
    private java.time.LocalDateTime lastLoginAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
