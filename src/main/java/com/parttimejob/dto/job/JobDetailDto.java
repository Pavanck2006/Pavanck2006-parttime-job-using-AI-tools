package com.parttimejob.dto.job;

import com.parttimejob.dto.application.ApplicationResponseDto;
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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailDto {
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
    
    // Detailed location - exposed if user is owner or accepted applicant
    private String detailedLocation;
    private boolean locationUnlocked;
    
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
    
    // Contact Info - exposed if user is owner or accepted applicant
    private String contactPhone;
    private String contactEmail;
    private boolean contactUnlocked;
    
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Contextual fields for requesting user
    private Boolean userApplied;
    private String userApplicationStatus;
    private Long userApplicationId;
    
    // Only populated for the job's owner or admin
    private List<ApplicationResponseDto> applications;
}
