package com.parttimejob.dto.payment;

import com.parttimejob.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentActionRequest {

    @NotNull(message = "Payment status action is required")
    private PaymentStatus status; // PAID (by owner) or CONFIRMED (by student)

    private String notes;
}
