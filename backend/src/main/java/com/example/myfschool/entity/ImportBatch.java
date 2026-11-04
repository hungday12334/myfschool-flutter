package com.example.myfschool.entity;

import com.example.myfschool.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="import_batches", indexes={@Index(name="idx_import_school_time", columnList="school_id,created_at"), @Index(name="idx_import_actor", columnList="imported_by")})
@Getter
@Setter
@NoArgsConstructor
public class ImportBatch extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="school_id", nullable=false)
    private School school;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="imported_by", nullable=false)
    private User importedBy;
    @Enumerated(EnumType.STRING)
    @Column(name="import_type", nullable=false)
    private ImportType importType;
    @Column(name="file_name", nullable=false, length=255)
    private String fileName;
    @Column(name="file_checksum", length=64)
    private String fileChecksum;
    @Column(name="total_rows", nullable=false)
    private int totalRows;
    @Column(name="success_rows", nullable=false)
    private int successRows;
    @Column(name="failed_rows", nullable=false)
    private int failedRows;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private ImportStatus status;
    @Column(name="started_at", insertable=false, updatable=false)
    private java.time.LocalDateTime startedAt;
    @Column(name="finished_at", insertable=false, updatable=false)
    private java.time.LocalDateTime finishedAt;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;
}
