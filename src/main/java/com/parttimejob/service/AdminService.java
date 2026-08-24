package com.parttimejob.service;

import com.parttimejob.dto.common.DashboardStatsDto;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.user.OwnerProfileDto;
import com.parttimejob.dto.user.StudentProfileDto;
import com.parttimejob.dto.user.UserManagementDto;
import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.OwnerProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.enums.ReportStatus;
import com.parttimejob.enums.Role;
import com.parttimejob.enums.VerificationStatus;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.*;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final CateringJobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ReportRepository reportRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalStudents = userRepository.countByRole(Role.ROLE_STUDENT);
        long totalOwners = userRepository.countByRole(Role.ROLE_OWNER);
        long verifiedOwners = ownerProfileRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long activeJobs = jobRepository.countByStatus(JobStatus.OPEN) + jobRepository.countByStatus(JobStatus.FILLED);
        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);
        long totalApps = applicationRepository.count();
        long pendingDisputes = reportRepository.countByStatus(ReportStatus.PENDING);
        long suspendedUsers = userRepository.countBySuspended(true);
        BigDecimal totalPayout = paymentRecordRepository.sumTotalPaidAmount();

        return DashboardStatsDto.builder()
                .totalStudentsCount(totalStudents)
                .totalOwnersCount(totalOwners)
                .verifiedOwnersCount(verifiedOwners)
                .totalActiveJobsCount(activeJobs)
                .totalCompletedJobsCount(completedJobs)
                .totalApplicationsCount(totalApps)
                .pendingDisputesCount(pendingDisputes)
                .suspendedUsersCount(suspendedUsers)
                .totalPlatformPayout(totalPayout != null ? totalPayout : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserManagementDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(dtoMapper::toUserManagementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentProfileDto> getAllStudents() {
        return studentProfileRepository.findAll().stream()
                .map(dtoMapper::toStudentProfileDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OwnerProfileDto> getAllOwners() {
        return ownerProfileRepository.findAll().stream()
                .map(dtoMapper::toOwnerProfileDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OwnerProfileDto verifyOwner(Long ownerId, boolean verified) {
        User admin = securityUtils.getCurrentUser();
        OwnerProfile owner = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + ownerId));

        if (verified) {
            owner.setVerificationStatus(VerificationStatus.VERIFIED);
            owner.setVerifiedAt(LocalDateTime.now());
        } else {
            owner.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
            owner.setVerifiedAt(null);
        }

        owner = ownerProfileRepository.save(owner);

        // Notify owner
        if (owner.getUser() != null) {
            String title = verified ? "Account Verified! Verified Badge Awarded" : "Account Verification Status Changed";
            String msg = verified ? "Congratulations! Your catering business account has been verified by Admin. The verified badge is now displayed on your job posts."
                    : "Your owner account verification status has been reverted to Pending Verification.";
            notificationService.sendNotification(owner.getUser(), title, msg, NotificationType.SYSTEM, owner.getId());
        }

        auditService.logAction(admin.getId(), "VERIFY_OWNER", "OwnerProfile", owner.getId(),
                "Admin changed verification status to " + owner.getVerificationStatus(), null);

        return dtoMapper.toOwnerProfileDto(owner);
    }

    @Transactional
    public UserManagementDto setUserSuspension(Long userId, boolean suspended, String reason) {
        User admin = securityUtils.getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setSuspended(suspended);
        user = userRepository.save(user);

        // Notify user
        String title = suspended ? "Account Suspended by Admin" : "Account Reactivated";
        String msg = suspended ? "Your account has been suspended by the platform administrator. Reason: " + (reason != null ? reason : "Terms violation")
                : "Your account suspension has been lifted. You may resume using the platform.";
        notificationService.sendNotification(user, title, msg, NotificationType.SYSTEM, user.getId());

        auditService.logAction(admin.getId(), "SUSPEND_USER", "User", user.getId(),
                "Admin set suspended=" + suspended + " for " + user.getEmail() + " Reason: " + reason, null);

        return dtoMapper.toUserManagementDto(user);
    }

    @Transactional(readOnly = true)
    public List<JobDetailDto> getAllJobs() {
        User admin = securityUtils.getCurrentUser();
        return jobRepository.findAll().stream()
                .map(job -> dtoMapper.toJobDetailDto(job, admin, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteJob(Long jobId, String reason) {
        User admin = securityUtils.getCurrentUser();
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);

        // Notify owner
        if (job.getOwner() != null && job.getOwner().getUser() != null) {
            notificationService.sendNotification(
                    job.getOwner().getUser(),
                    "Job Removed by Admin: " + job.getTitle(),
                    "Your job posting '" + job.getTitle() + "' was removed by administrator moderation. Reason: " + (reason != null ? reason : "Moderation policy"),
                    NotificationType.JOB_CANCELLED,
                    job.getId()
            );
        }

        auditService.logAction(admin.getId(), "DELETE_JOB_ADMIN", "CateringJob", job.getId(),
                "Admin removed job: " + job.getTitle() + " Reason: " + reason, null);
    }
}
