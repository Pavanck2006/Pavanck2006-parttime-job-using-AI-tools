package com.parttimejob.dto.job;

import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.VerificationStatus;
import com.parttimejob.enums.WorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPublicDto {
    private Long id;
    private Long ownerId;
    private String cateringName;
    private String ownerName;
    private VerificationStatus ownerVerificationStatus;
    private boolean ownerVerified;
    
    private String title;
    private String description;
    private WorkType workType;
    private String workTypeDisplayName;
    private String workArea;
    // Approximate / masked location for privacy
    private String approximateLocation;
    
    private LocalDate jobDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal paymentAmount;
    private PaymentType paymentType;
    private String paymentTypeDisplayName;
    private boolean onSpotPayment;
    
    private Integer workersRequired;
    private Integer workersSelected;
    private String requiredSkills;
    private JobStatus status;
    private LocalDateTime createdAt;
    
    // User contextual fields
    private Boolean userApplied;
    private String userApplicationStatus;
    private Long userApplicationId;
}
