package com.parttimejob.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    // Student Stats
    private Long availableJobsCount;
    private Long appliedJobsCount;
    private Long acceptedJobsCount;
    private Long completedJobsCount;
    private Long pendingPaymentsCount;
    private BigDecimal totalEarnings;

    // Owner Stats
    private Long totalJobsPosted;
    private Long activeJobsCount;
    private Long completedJobsOwnerCount;
    private Long totalWorkersHired;
    private Long pendingApplicantCount;
    private BigDecimal totalPaidAmount;
    private String verificationStatus;

    // Admin Stats
    private Long totalStudentsCount;
    private Long totalOwnersCount;
    private Long verifiedOwnersCount;
    private Long totalActiveJobsCount;
    private Long totalCompletedJobsCount;
    private Long totalApplicationsCount;
    private Long pendingDisputesCount;
    private Long suspendedUsersCount;
    private BigDecimal totalPlatformPayout;
}
