package com.parttimejob.mapper;

import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.dto.auth.UserSummaryDto;
import com.parttimejob.dto.common.AuditLogDto;
import com.parttimejob.dto.common.NotificationDto;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.job.JobPublicDto;
import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.dto.report.ReportResponseDto;
import com.parttimejob.dto.user.OwnerProfileDto;
import com.parttimejob.dto.user.StudentProfileDto;
import com.parttimejob.dto.user.UserManagementDto;
import com.parttimejob.entity.*;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.Role;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoMapper {

    public JobPublicDto toJobPublicDto(CateringJob job, Long currentStudentProfileId) {
        if (job == null) return null;

        Boolean userApplied = false;
        String userAppStatus = null;
        Long userAppId = null;

        if (currentStudentProfileId != null && job.getApplications() != null) {
            for (JobApplication app : job.getApplications()) {
                if (app.getStudent() != null && currentStudentProfileId.equals(app.getStudent().getId())) {
                    userApplied = true;
                    userAppStatus = app.getStatus().name();
                    userAppId = app.getId();
                    break;
                }
            }
        }

        OwnerProfile owner = job.getOwner();
        String cateringName = (owner != null) ? owner.getCateringName() : "Catering Service";
        String ownerName = (owner != null && owner.getUser() != null) ? owner.getUser().getFullName() : "Catering Owner";
        boolean isOwnerVerified = (owner != null && owner.isVerified());

        return JobPublicDto.builder()
                .id(job.getId())
                .ownerId(owner != null ? owner.getId() : null)
                .cateringName(cateringName)
                .ownerName(ownerName)
                .ownerVerificationStatus(owner != null ? owner.getVerificationStatus() : null)
                .ownerVerified(isOwnerVerified)
                .title(job.getTitle())
                .description(job.getDescription())
                .workType(job.getWorkType())
                .workTypeDisplayName(job.getWorkType() != null ? job.getWorkType().getDisplayName() : "")
                .workArea(job.getWorkArea())
                .approximateLocation("Area: " + job.getWorkArea() + " (Exact address disclosed after application acceptance)")
                .jobDate(job.getJobDate())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .paymentAmount(job.getPaymentAmount())
                .paymentType(job.getPaymentType())
                .paymentTypeDisplayName(job.getPaymentType() != null ? job.getPaymentType().getDisplayName() : "")
                .onSpotPayment(job.isOnSpotPayment())
                .workersRequired(job.getWorkersRequired())
                .workersSelected(job.getWorkersSelected())
                .requiredSkills(job.getRequiredSkills())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .userApplied(userApplied)
                .userApplicationStatus(userAppStatus)
                .userApplicationId(userAppId)
                .build();
    }

    public JobDetailDto toJobDetailDto(CateringJob job, User currentUser, Long currentStudentProfileId) {
        if (job == null) return null;

        OwnerProfile owner = job.getOwner();
        String cateringName = (owner != null) ? owner.getCateringName() : "Catering Service";
        String ownerName = (owner != null && owner.getUser() != null) ? owner.getUser().getFullName() : "Catering Owner";
        boolean isOwnerVerified = (owner != null && owner.isVerified());

        boolean isOwner = (currentUser != null && owner != null && owner.getUser() != null &&
                owner.getUser().getId().equals(currentUser.getId()));
        boolean isAdmin = (currentUser != null && currentUser.getRole() == Role.ROLE_ADMIN);

        boolean isAcceptedStudent = false;
        Boolean userApplied = false;
        String userAppStatus = null;
        Long userAppId = null;

        if (currentStudentProfileId != null && job.getApplications() != null) {
            for (JobApplication app : job.getApplications()) {
                if (app.getStudent() != null && currentStudentProfileId.equals(app.getStudent().getId())) {
                    userApplied = true;
                    userAppStatus = app.getStatus().name();
                    userAppId = app.getId();
                    if (app.getStatus() == ApplicationStatus.ACCEPTED || app.getStatus() == ApplicationStatus.COMPLETED) {
                        isAcceptedStudent = true;
                    }
                    break;
                }
            }
        }

        boolean canViewSensitiveDetails = isOwner || isAdmin || isAcceptedStudent;

        List<ApplicationResponseDto> applicationDtos = Collections.emptyList();
        if ((isOwner || isAdmin) && job.getApplications() != null) {
            applicationDtos = job.getApplications().stream()
                    .map(this::toApplicationResponseDto)
                    .collect(Collectors.toList());
        }

        return JobDetailDto.builder()
                .id(job.getId())
                .ownerId(owner != null ? owner.getId() : null)
                .cateringName(cateringName)
                .ownerName(ownerName)
                .ownerVerificationStatus(owner != null ? owner.getVerificationStatus() : null)
                .ownerVerified(isOwnerVerified)
                .title(job.getTitle())
                .description(job.getDescription())
                .workType(job.getWorkType())
                .workTypeDisplayName(job.getWorkType() != null ? job.getWorkType().getDisplayName() : "")
                .workArea(job.getWorkArea())
                .detailedLocation(canViewSensitiveDetails ? job.getDetailedLocation() : "[Address details will be unlocked once accepted]")
                .locationUnlocked(canViewSensitiveDetails)
                .jobDate(job.getJobDate())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .paymentAmount(job.getPaymentAmount())
                .paymentType(job.getPaymentType())
                .paymentTypeDisplayName(job.getPaymentType() != null ? job.getPaymentType().getDisplayName() : "")
                .onSpotPayment(job.isOnSpotPayment())
                .workersRequired(job.getWorkersRequired())
                .workersSelected(job.getWorkersSelected())
                .requiredSkills(job.getRequiredSkills())
                .contactPhone(canViewSensitiveDetails ? job.getContactPhone() : "[Phone unlocked after acceptance]")
                .contactEmail(canViewSensitiveDetails ? job.getContactEmail() : "[Email unlocked after acceptance]")
                .contactUnlocked(canViewSensitiveDetails)
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .userApplied(userApplied)
                .userApplicationStatus(userAppStatus)
                .userApplicationId(userAppId)
                .applications(applicationDtos)
                .build();
    }

    public ApplicationResponseDto toApplicationResponseDto(JobApplication app) {
        if (app == null) return null;

        CateringJob job = app.getJob();
        StudentProfile student = app.getStudent();
        User studentUser = (student != null) ? student.getUser() : null;
        OwnerProfile owner = (job != null) ? job.getOwner() : null;
        User ownerUser = (owner != null) ? owner.getUser() : null;

        boolean isAccepted = (app.getStatus() == ApplicationStatus.ACCEPTED || app.getStatus() == ApplicationStatus.COMPLETED);

        return ApplicationResponseDto.builder()
                .id(app.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .workType(job != null ? job.getWorkType() : null)
                .workTypeDisplayName(job != null && job.getWorkType() != null ? job.getWorkType().getDisplayName() : "")
                .workArea(job != null ? job.getWorkArea() : null)
                .detailedLocation(isAccepted && job != null ? job.getDetailedLocation() : "[Disclosed after acceptance]")
                .locationUnlocked(isAccepted)
                .jobDate(job != null ? job.getJobDate() : null)
                .startTime(job != null ? job.getStartTime() : null)
                .endTime(job != null ? job.getEndTime() : null)
                .paymentType(job != null ? job.getPaymentType() : null)
                .paymentTypeDisplayName(job != null && job.getPaymentType() != null ? job.getPaymentType().getDisplayName() : "")
                .onSpotPayment(job != null && job.isOnSpotPayment())
                .contactPhone(isAccepted && job != null ? job.getContactPhone() : "[Disclosed after acceptance]")
                .contactEmail(isAccepted && job != null ? job.getContactEmail() : "[Disclosed after acceptance]")
                .contactUnlocked(isAccepted)
                .ownerId(owner != null ? owner.getId() : null)
                .cateringName(owner != null ? owner.getCateringName() : "Catering")
                .ownerName(ownerUser != null ? ownerUser.getFullName() : "Owner")
                .ownerVerified(owner != null && owner.isVerified())
                .studentId(student != null ? student.getId() : null)
                .studentUserId(studentUser != null ? studentUser.getId() : null)
                .studentName(studentUser != null ? studentUser.getFullName() : "Student")
                .studentEmail(studentUser != null ? studentUser.getEmail() : null)
                .studentPhone(studentUser != null ? studentUser.getPhone() : null)
                .collegeName(student != null ? student.getCollegeName() : null)
                .skills(student != null ? student.getSkills() : null)
                .studentRating(student != null ? student.getRating() : 5.0)
                .totalJobsCompleted(student != null ? student.getTotalJobsCompleted() : 0)
                .status(app.getStatus())
                .attendanceStatus(app.getAttendanceStatus())
                .workCompletionStatus(app.getWorkCompletionStatus())
                .paymentStatus(app.getPaymentStatus())
                .paymentAmount(app.getPaymentAmount())
                .paymentConfirmationDate(app.getPaymentConfirmationDate())
                .notes(app.getNotes())
                .appliedAt(app.getAppliedAt())
                .respondedAt(app.getRespondedAt())
                .createdAt(app.getCreatedAt())
                .build();
    }

    public PaymentResponseDto toPaymentResponseDto(PaymentRecord pay) {
        if (pay == null) return null;

        JobApplication app = pay.getApplication();
        CateringJob job = pay.getJob();
        StudentProfile student = pay.getStudent();
        OwnerProfile owner = pay.getOwner();

        return PaymentResponseDto.builder()
                .id(pay.getId())
                .applicationId(app != null ? app.getId() : null)
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : "Catering Job")
                .workArea(job != null ? job.getWorkArea() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null && student.getUser() != null ? student.getUser().getFullName() : "Student")
                .studentEmail(student != null && student.getUser() != null ? student.getUser().getEmail() : null)
                .ownerId(owner != null ? owner.getId() : null)
                .ownerCateringName(owner != null ? owner.getCateringName() : "Catering Service")
                .ownerName(owner != null && owner.getUser() != null ? owner.getUser().getFullName() : "Owner")
                .amount(pay.getAmount())
                .paymentType(pay.getPaymentType())
                .paymentTypeDisplayName(pay.getPaymentType() != null ? pay.getPaymentType().getDisplayName() : "")
                .paymentStatus(pay.getPaymentStatus())
                .markedPaidAt(pay.getMarkedPaidAt())
                .confirmedPaidAt(pay.getConfirmedPaidAt())
                .notes(pay.getNotes())
                .createdAt(pay.getCreatedAt())
                .build();
    }

    public ReportResponseDto toReportResponseDto(Report report) {
        if (report == null) return null;

        User reporter = report.getReporter();
        User target = report.getTargetUser();
        CateringJob job = report.getJob();
        JobApplication app = report.getApplication();

        String targetCatering = null;
        if (target != null && target.getOwnerProfile() != null) {
            targetCatering = target.getOwnerProfile().getCateringName();
        }

        return ReportResponseDto.builder()
                .id(report.getId())
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getFullName() : "Reporter")
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reporterRole(reporter != null ? reporter.getRole() : null)
                .targetUserId(target != null ? target.getId() : null)
                .targetUserName(target != null ? target.getFullName() : "Target")
                .targetUserEmail(target != null ? target.getEmail() : null)
                .targetUserRole(target != null ? target.getRole() : null)
                .targetCateringName(targetCatering)
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : "N/A")
                .applicationId(app != null ? app.getId() : null)
                .reportType(report.getReportType())
                .reportTypeDisplayName(report.getReportType() != null ? report.getReportType().getDisplayName() : "")
                .description(report.getDescription())
                .expectedAmount(report.getExpectedAmount())
                .receivedAmount(report.getReceivedAmount())
                .evidenceNotes(report.getEvidenceNotes())
                .status(report.getStatus())
                .adminRemarks(report.getAdminRemarks())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public StudentProfileDto toStudentProfileDto(StudentProfile profile) {
        if (profile == null) return null;
        User user = profile.getUser();

        return StudentProfileDto.builder()
                .id(profile.getId())
                .userId(user != null ? user.getId() : null)
                .email(user != null ? user.getEmail() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phone(user != null ? user.getPhone() : null)
                .collegeName(profile.getCollegeName())
                .preferredArea(profile.getPreferredArea())
                .skills(profile.getSkills())
                .bio(profile.getBio())
                .emergencyContact(profile.getEmergencyContact())
                .totalJobsCompleted(profile.getTotalJobsCompleted())
                .rating(profile.getRating())
                .active(user != null && user.isActive())
                .suspended(user != null && user.isSuspended())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    public OwnerProfileDto toOwnerProfileDto(OwnerProfile profile) {
        if (profile == null) return null;
        User user = profile.getUser();

        return OwnerProfileDto.builder()
                .id(profile.getId())
                .userId(user != null ? user.getId() : null)
                .email(user != null ? user.getEmail() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phone(user != null ? user.getPhone() : null)
                .cateringName(profile.getCateringName())
                .businessAddress(profile.getBusinessAddress())
                .businessPhone(profile.getBusinessPhone())
                .verificationStatus(profile.getVerificationStatus())
                .verified(profile.isVerified())
                .verifiedAt(profile.getVerifiedAt())
                .totalJobsPosted(profile.getTotalJobsPosted())
                .active(user != null && user.isActive())
                .suspended(user != null && user.isSuspended())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    public UserManagementDto toUserManagementDto(User user) {
        if (user == null) return null;

        Long profileId = null;
        String college = null;
        String catering = null;
        com.parttimejob.enums.VerificationStatus verificationStatus = null;
        int jobsCount = 0;

        if (user.getRole() == Role.ROLE_STUDENT && user.getStudentProfile() != null) {
            profileId = user.getStudentProfile().getId();
            college = user.getStudentProfile().getCollegeName();
            jobsCount = user.getStudentProfile().getTotalJobsCompleted();
        } else if (user.getRole() == Role.ROLE_OWNER && user.getOwnerProfile() != null) {
            profileId = user.getOwnerProfile().getId();
            catering = user.getOwnerProfile().getCateringName();
            verificationStatus = user.getOwnerProfile().getVerificationStatus();
            jobsCount = user.getOwnerProfile().getTotalJobsPosted();
        }

        return UserManagementDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .suspended(user.isSuspended())
                .createdAt(user.getCreatedAt())
                .profileId(profileId)
                .collegeName(college)
                .cateringName(catering)
                .verificationStatus(verificationStatus)
                .jobsCount(jobsCount)
                .build();
    }

    public NotificationDto toNotificationDto(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .relatedEntityId(n.getRelatedEntityId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    public AuditLogDto toAuditLogDto(AuditLog log) {
        if (log == null) return null;
        return AuditLogDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }

    public UserSummaryDto toUserSummaryDto(User user) {
        if (user == null) return null;
        return UserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .suspended(user.isSuspended())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
