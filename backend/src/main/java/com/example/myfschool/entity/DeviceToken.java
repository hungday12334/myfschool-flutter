package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="device_tokens", uniqueConstraints={@UniqueConstraint(name="uk_device_token", columnNames={"token"})}, indexes={@Index(name="idx_device_user_active", columnList="user_id,is_active")})
@Getter
@Setter
@NoArgsConstructor
public class DeviceToken extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="token", nullable=false, length=512)
    private String token;
    @Enumerated(EnumType.STRING)
    @Column(name="platform", nullable=false)
    private DevicePlatform platform;
    @Column(name="device_name", length=150)
    private String deviceName;
    @Column(name="is_active", nullable=false)
    private boolean isActive;
    @Column(name="last_seen_at", insertable=false, updatable=false)
    private java.time.LocalDateTime lastSeenAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
