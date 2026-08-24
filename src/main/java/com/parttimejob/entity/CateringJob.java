package com.parttimejob.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.WorkType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "catering_jobs", indexes = {
        @Index(name = "idx_job_area", columnList = "work_area"),
        @Index(name = "idx_job_date", columnList = "job_date"),
        @Index(name = "idx_job_work_type", columnList = "work_type"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_payment", columnList = "payment_amount")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CateringJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private OwnerProfile owner;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 50)
    private WorkType workType;

    @Column(name = "work_area", nullable = false, length = 100)
    private String workArea;

    @Column(name = "detailed_location", nullable = false, columnDefinition = "TEXT")
    private String detailedLocation;

    @Column(name = "job_date", nullable = false)
    private LocalDate jobDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "payment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 50)
    private PaymentType paymentType;

    @Builder.Default
    @Column(name = "is_on_spot_payment", nullable = false)
    private boolean onSpotPayment = true;

    @Column(name = "workers_required", nullable = false)
    private Integer workersRequired;

    @Builder.Default
    @Column(name = "workers_selected", nullable = false)
    private Integer workersSelected = 0;

    @Column(name = "required_skills", length = 255)
    private String requiredSkills;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private JobStatus status = JobStatus.OPEN;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<JobApplication> applications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean canAcceptMoreWorkers() {
        return this.workersSelected < this.workersRequired;
    }
}
