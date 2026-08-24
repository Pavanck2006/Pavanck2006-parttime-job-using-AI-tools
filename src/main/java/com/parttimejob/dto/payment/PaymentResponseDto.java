package com.parttimejob.dto.payment;

import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String workArea;
    
    private Long studentId;
    private String studentName;
    private String studentEmail;
    
    private Long ownerId;
    private String ownerCateringName;
    private String ownerName;
    
    private BigDecimal amount;
    private PaymentType paymentType;
    private String paymentTypeDisplayName;
    private PaymentStatus paymentStatus;
    private LocalDateTime markedPaidAt;
    private LocalDateTime confirmedPaidAt;
    private String notes;
    private LocalDateTime createdAt;
}
