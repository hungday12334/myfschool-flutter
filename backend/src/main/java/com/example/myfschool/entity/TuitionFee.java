package com.example.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tuition_fees",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tuition_student_year_code",
                columnNames = {"student_id", "academic_year", "fee_code"}
        ),
        indexes = @Index(
                name = "idx_tuition_student_due_date",
                columnList = "student_id,due_date"
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TuitionFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "fee_code", nullable = false, length = 50)
    private String feeCode;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;
}
