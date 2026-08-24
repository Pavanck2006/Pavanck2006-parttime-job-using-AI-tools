package com.parttimejob.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.AttendanceStatus;
import com.parttimejob.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_job_student", columnNames = {"job_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_app_status", columnList = "status"),
                @Index(name = "idx_app_payment_status", columnList = "payment_status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnore
    private CateringJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private StudentProfile student;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "attendance_status", nullable = false, length = 30)
    private AttendanceStatus attendanceStatus = AttendanceStatus.NOT_MARKED;

    @Builder.Default
    @Column(name = "work_completion_status", length = 30)
    private String workCompletionStatus = "NOT_COMPLETED";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_confirmation_date")
    private LocalDateTime paymentConfirmationDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private PaymentRecord paymentRecord;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
