package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="parents", uniqueConstraints={@UniqueConstraint(name="uk_parent_user", columnNames={"user_id"})}, indexes={@Index(name="idx_parent_school", columnList="school_id")})
@Getter
@Setter
@NoArgsConstructor
public class Parent extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="full_name", nullable=false, length=150)
    private String fullName;
    @Column(name="phone", length=20)
    private String phone;
    @Column(name="email", length=255)
    private String email;
    @Column(name="address", length=500)
    private String address;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
