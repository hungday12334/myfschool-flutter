package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="audit_logs", indexes={@Index(name="idx_audit_school_time", columnList="school_id,created_at"), @Index(name="idx_audit_actor_time", columnList="actor_user_id,created_at"), @Index(name="idx_audit_entity", columnList="entity_type,entity_id")})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="school_id")
    private School school;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actor_user_id")
    private User actor;
    @Column(name="action", nullable=false, length=100)
    private String action;
    @Column(name="entity_type", nullable=false, length=100)
    private String entityType;
    @Column(name="entity_id")
    private Long entityId;
    @Column(name="old_values", columnDefinition="JSON")
    private String oldValues;
    @Column(name="new_values", columnDefinition="JSON")
    private String newValues;
    @Column(name="ip_address", length=45)
    private String ipAddress;
    @Column(name="user_agent", length=500)
    private String userAgent;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
}
