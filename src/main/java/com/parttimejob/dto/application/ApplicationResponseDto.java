package com.parttimejob.dto.application;

import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.AttendanceStatus;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.PaymentType;
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
public class ApplicationResponseDto {
    private Long id;
    
    // Job details
    private Long jobId;
    private String jobTitle;
    private WorkType workType;
    private String workTypeDisplayName;
    private String workArea;
    private String detailedLocation;
    private boolean locationUnlocked;
    private LocalDate jobDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private PaymentType paymentType;
    private String paymentTypeDisplayName;
    private boolean onSpotPayment;
    private String contactPhone;
    private String contactEmail;
    private boolean contactUnlocked;
    
    // Owner details
    private Long ownerId;
    private String cateringName;
    private String ownerName;
    private boolean ownerVerified;
    
    // Student details
    private Long studentId;
    private Long studentUserId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String collegeName;
    private String skills;
    private Double studentRating;
    private int totalJobsCompleted;
    
    // Application Lifecycle
    private ApplicationStatus status;
    private AttendanceStatus attendanceStatus;
    private String workCompletionStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentConfirmationDate;
    private String notes;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
}
