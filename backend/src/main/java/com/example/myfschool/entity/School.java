package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="schools", uniqueConstraints={@UniqueConstraint(name="uk_school_code", columnNames={"school_code"})})
@Getter
@Setter
@NoArgsConstructor
public class School extends BaseEntity {
    @Column(name="school_code", nullable=false, length=40)
    private String schoolCode;
    @Column(name="name", nullable=false, length=255)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name="school_level", nullable=false)
    private SchoolLevel schoolLevel;
    @Column(name="address", length=500)
    private String address;
    @Column(name="phone", length=20)
    private String phone;
    @Column(name="email", length=255)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private SchoolStatus status;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
