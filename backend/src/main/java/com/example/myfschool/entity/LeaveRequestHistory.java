package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="leave_request_histories", indexes={@Index(name="idx_leave_history_request_time", columnList="leave_request_id,changed_at"), @Index(name="idx_leave_history_actor", columnList="actor_user_id")})
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestHistory extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="leave_request_id", nullable=false)
    private LeaveRequest leaveRequest;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="actor_user_id", nullable=false)
    private User actor;
    @Enumerated(EnumType.STRING)
    @Column(name="old_status")
    private LeaveRequestStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(name="new_status", nullable=false)
    private LeaveRequestStatus newStatus;
    @Column(name="note", columnDefinition="TEXT")
    private String note;
    @Column(name="changed_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime changedAt;
}
