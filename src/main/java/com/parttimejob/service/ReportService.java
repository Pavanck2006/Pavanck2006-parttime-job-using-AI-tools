package com.parttimejob.service;

import com.parttimejob.dto.report.ReportCreateRequest;
import com.parttimejob.dto.report.ReportResolveRequest;
import com.parttimejob.dto.report.ReportResponseDto;
import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.Report;
import com.parttimejob.entity.User;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.ReportStatus;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.CateringJobRepository;
import com.parttimejob.repository.JobApplicationRepository;
import com.parttimejob.repository.ReportRepository;
import com.parttimejob.repository.UserRepository;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CateringJobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public ReportResponseDto createReport(ReportCreateRequest request) {
        User reporter = securityUtils.getCurrentUser();

        CateringJob job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId()).orElse(null);
        }

        JobApplication application = null;
        if (request.getApplicationId() != null) {
            application = applicationRepository.findById(request.getApplicationId()).orElse(null);
            if (job == null && application != null) {
                job = application.getJob();
            }
        }

        User targetUser = null;
        if (request.getTargetUserId() != null) {
            targetUser = userRepository.findById(request.getTargetUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + request.getTargetUserId()));
        } else if (job != null && job.getOwner() != null && job.getOwner().getUser() != null) {
            targetUser = job.getOwner().getUser();
        } else if (application != null && application.getStudent() != null && application.getStudent().getUser() != null) {
            targetUser = application.getStudent().getUser();
        }

        if (targetUser == null) {
            throw new BadRequestException("Target user for report could not be determined. Please specify target user ID or valid job/application.");
        }

        if (targetUser.getId().equals(reporter.getId())) {
            throw new BadRequestException("You cannot file a report against yourself.");
        }

        // If it is a payment issue on an application, mark application status as DISPUTED
        if (application != null && (request.getReportType() == com.parttimejob.enums.ReportType.PAYMENT_NOT_RECEIVED ||
                request.getReportType() == com.parttimejob.enums.ReportType.PAYMENT_PARTIALLY_RECEIVED)) {
            application.setPaymentStatus(PaymentStatus.DISPUTED);
            applicationRepository.save(application);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetUser(targetUser)
                .job(job)
                .application(application)
                .reportType(request.getReportType())
                .description(request.getDescription().trim())
                .expectedAmount(request.getExpectedAmount())
                .receivedAmount(request.getReceivedAmount())
                .evidenceNotes(request.getEvidenceNotes() != null ? request.getEvidenceNotes().trim() : null)
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);

        // Notify target user
        notificationService.sendNotification(
                targetUser,
                "Complaint Filed: " + request.getReportType().getDisplayName(),
                "A formal report (" + request.getReportType().getDisplayName() + ") has been submitted regarding " +
                        (job != null ? "'" + job.getTitle() + "'" : "recent interactions") + ". Admin moderation is reviewing.",
                NotificationType.REPORT_FILED,
                report.getId()
        );

        auditService.logAction(reporter.getId(), "CREATE_REPORT", "Report", report.getId(),
                "Filed report of type " + request.getReportType() + " against user " + targetUser.getEmail(), null);

        return dtoMapper.toReportResponseDto(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getMyReports() {
        User user = securityUtils.getCurrentUser();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(dtoMapper::toReportResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getAllReports() {
        User user = securityUtils.getCurrentUser();
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new ForbiddenException("Only administrators can view all reports.");
        }
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(dtoMapper::toReportResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReportResponseDto resolveReport(Long reportId, ReportResolveRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new ForbiddenException("Only administrators can resolve reports.");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        report.setStatus(request.getStatus());
        report.setAdminRemarks(request.getAdminRemarks().trim());
        report.setResolvedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        // Notify reporter
        if (report.getReporter() != null) {
            notificationService.sendNotification(
                    report.getReporter(),
                    "Report Resolved: " + report.getReportType().getDisplayName(),
                    "Your report has been " + request.getStatus().name().toLowerCase() + " by Admin. Remarks: " + request.getAdminRemarks(),
                    NotificationType.REPORT_RESOLVED,
                    report.getId()
            );
        }

        // Notify target user
        if (report.getTargetUser() != null) {
            notificationService.sendNotification(
                    report.getTargetUser(),
                    "Report Update: " + report.getReportType().getDisplayName(),
                    "The report concerning your account was " + request.getStatus().name().toLowerCase() + " by Admin. Remarks: " + request.getAdminRemarks(),
                    NotificationType.REPORT_RESOLVED,
                    report.getId()
            );
        }

        auditService.logAction(user.getId(), "RESOLVE_REPORT", "Report", report.getId(),
                "Admin resolved report to " + request.getStatus() + ": " + request.getAdminRemarks(), null);

        return dtoMapper.toReportResponseDto(report);
    }
}
