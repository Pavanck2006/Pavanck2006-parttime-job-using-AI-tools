package com.parttimejob.dto.report;

import com.parttimejob.enums.ReportStatus;
import com.parttimejob.enums.ReportType;
import com.parttimejob.enums.Role;
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
public class ReportResponseDto {
    private Long id;
    
    // Reporter
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private Role reporterRole;
    
    // Target user
    private Long targetUserId;
    private String targetUserName;
    private String targetUserEmail;
    private Role targetUserRole;
    private String targetCateringName;
    
    // Job context
    private Long jobId;
    private String jobTitle;
    private Long applicationId;
    
    // Report info
    private ReportType reportType;
    private String reportTypeDisplayName;
    private String description;
    private BigDecimal expectedAmount;
    private BigDecimal receivedAmount;
    private String evidenceNotes;
    private ReportStatus status;
    private String adminRemarks;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
