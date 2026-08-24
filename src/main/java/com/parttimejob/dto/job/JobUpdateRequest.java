package com.parttimejob.dto.job;

import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.WorkType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobUpdateRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    private String description;

    @NotNull(message = "Work type is required")
    private WorkType workType;

    @NotBlank(message = "Work area is required")
    private String workArea;

    @NotBlank(message = "Detailed location is required")
    private String detailedLocation;

    @NotNull(message = "Job date is required")
    private LocalDate jobDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "100.00", message = "Payment amount must be at least ₹100")
    private BigDecimal paymentAmount;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    private boolean onSpotPayment;

    @NotNull(message = "Number of workers required is mandatory")
    @Min(value = 1, message = "At least 1 worker must be required")
    private Integer workersRequired;

    private String requiredSkills;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    private String contactEmail;
}
