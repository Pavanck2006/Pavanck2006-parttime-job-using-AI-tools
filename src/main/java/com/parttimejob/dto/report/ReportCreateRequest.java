package com.parttimejob.dto.report;

import com.parttimejob.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {

    private Long targetUserId;

    private Long jobId;

    private Long applicationId;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotBlank(message = "Description of the problem is required")
    private String description;

    private BigDecimal expectedAmount;

    private BigDecimal receivedAmount;

    private String evidenceNotes;
}
