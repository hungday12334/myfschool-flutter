package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="announcements", indexes={@Index(name="idx_announcement_school_status", columnList="school_id,status,published_at"), @Index(name="idx_announcement_author", columnList="author_user_id")})
@Getter
@Setter
@NoArgsConstructor
public class Announcement extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="author_user_id", nullable=false)
    private User author;
    @Column(name="title", nullable=false, length=255)
    private String title;
    @Column(name="content", nullable=false, columnDefinition="LONGTEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private AnnouncementStatus status;
    @Column(name="published_at", insertable=false, updatable=false)
    private java.time.LocalDateTime publishedAt;
    @Column(name="expires_at", insertable=false, updatable=false)
    private java.time.LocalDateTime expiresAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
