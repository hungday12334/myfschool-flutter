package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="roles", uniqueConstraints={@UniqueConstraint(name="uk_role_code", columnNames={"code"})})
@Getter
@Setter
@NoArgsConstructor
public class Role extends BaseEntity {
    @Column(name="code", nullable=false, length=40)
    private String code;
    @Column(name="name", nullable=false, length=100)
    private String name;
    @Column(name="description", length=500)
    private String description;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
}
