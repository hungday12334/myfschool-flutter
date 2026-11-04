package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="timetable_entries", indexes={@Index(name="idx_timetable_assignment", columnList="teaching_assignment_id"), @Index(name="idx_timetable_slot", columnList="day_of_week,shift,period_number")})
@Getter
@Setter
@NoArgsConstructor
public class TimetableEntry extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="teaching_assignment_id", nullable=false)
    private TeachingAssignment teachingAssignment;
    @Column(name="day_of_week", nullable=false)
    private int dayOfWeek;
    @Column(name="period_number", nullable=false)
    private int periodNumber;
    @Enumerated(EnumType.STRING)
    @Column(name="shift", nullable=false)
    private Shift shift;
    @Column(name="room", length=50)
    private String room;
    @Column(name="effective_from", nullable=false)
    private java.time.LocalDate effectiveFrom;
    @Column(name="effective_to")
    private java.time.LocalDate effectiveTo;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
