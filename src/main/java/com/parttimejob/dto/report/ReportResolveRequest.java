package com.parttimejob.dto.report;

import com.parttimejob.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResolveRequest {

    @NotNull(message = "Resolution status is required (RESOLVED or DISMISSED)")
    private ReportStatus status;

    @NotBlank(message = "Admin remarks are required when resolving a report")
    private String adminRemarks;
}
