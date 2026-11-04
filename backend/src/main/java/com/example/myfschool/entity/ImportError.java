package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="import_errors", indexes={@Index(name="idx_import_error_batch_row", columnList="batch_id,row_number")})
@Getter
@Setter
@NoArgsConstructor
public class ImportError extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="batch_id", nullable=false)
    private ImportBatch batch;
    @Column(name="row_number", nullable=false)
    private int rowNumber;
    @Column(name="column_name", length=100)
    private String columnName;
    @Column(name="error_code", length=60)
    private String errorCode;
    @Column(name="error_message", nullable=false, columnDefinition="TEXT")
    private String errorMessage;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
}
