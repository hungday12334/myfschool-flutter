package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="announcement_reads", uniqueConstraints={@UniqueConstraint(name="uk_announcement_read", columnNames={"announcement_id", "user_id"})}, indexes={@Index(name="idx_read_user", columnList="user_id")})
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementRead extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="announcement_id", nullable=false)
    private Announcement announcement;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="read_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime readAt;
}
