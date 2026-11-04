package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="announcement_targets", indexes={@Index(name="idx_target_announcement", columnList="announcement_id"), @Index(name="idx_target_school", columnList="school_id"), @Index(name="idx_target_class", columnList="class_id"), @Index(name="idx_target_user", columnList="user_id")})
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementTarget extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="announcement_id", nullable=false)
    private Announcement announcement;
    @Enumerated(EnumType.STRING)
    @Column(name="target_type", nullable=false)
    private AnnouncementTargetType targetType;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="school_id")
    private School school;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="class_id")
    private SchoolClass schoolClass;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
}
