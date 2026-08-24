package com.parttimejob.service;

import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.dto.application.AttendanceUpdateRequest;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.dto.user.OwnerProfileDto;
import com.parttimejob.dto.user.ProfileUpdateRequest;
import com.parttimejob.entity.*;
import com.parttimejob.enums.*;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.*;
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
public class OwnerService {

    private final OwnerProfileRepository ownerProfileRepository;
    private final CateringJobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public OwnerProfileDto getOwnerProfile() {
        User user = securityUtils.getCurrentUser();
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found."));
        return dtoMapper.toOwnerProfileDto(owner);
    }

    @Transactional
    public OwnerProfileDto updateOwnerProfile(ProfileUpdateRequest request) {
        User user = securityUtils.getCurrentUser();
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found."));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getCateringName() != null && !request.getCateringName().trim().isEmpty()) {
            owner.setCateringName(request.getCateringName().trim());
        }
        if (request.getBusinessAddress() != null) {
            owner.setBusinessAddress(request.getBusinessAddress().trim());
        }
        if (request.getBusinessPhone() != null && !request.getBusinessPhone().trim().isEmpty()) {
            owner.setBusinessPhone(request.getBusinessPhone().trim());
        }

        ownerProfileRepository.save(owner);
        auditService.logAction(user.getId(), "UPDATE_OWNER_PROFILE", "OwnerProfile", owner.getId(), "Updated owner profile", null);

        return dtoMapper.toOwnerProfileDto(owner);
    }

    @Transactional(readOnly = true)
    public List<JobDetailDto> getMyJobs() {
        User user = securityUtils.getCurrentUser();
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found."));

        return jobRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(job -> dtoMapper.toJobDetailDto(job, user, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getJobApplicants(Long jobId) {
        User user = securityUtils.getCurrentUser();
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to view applicants for this job.");
        }

        return applicationRepository.findByJobIdOrderByAppliedAtAsc(jobId)
                .stream()
                .map(dtoMapper::toApplicationResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponseDto acceptApplicant(Long applicationId) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        CateringJob job = application.getJob();
        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to accept applicants for this job.");
        }

        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            return dtoMapper.toApplicationResponseDto(application);
        }

        // Strict Worker Limit Enforcement
        if (job.getWorkersSelected() >= job.getWorkersRequired()) {
            throw new BadRequestException("Worker limit reached! This job requires " + job.getWorkersRequired() +
                    " workers and already has " + job.getWorkersSelected() + " accepted workers.");
        }

        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setRespondedAt(LocalDateTime.now());
        application = applicationRepository.save(application);

        // Increment selected count on job
        job.setWorkersSelected(job.getWorkersSelected() + 1);
        if (job.getWorkersSelected() >= job.getWorkersRequired()) {
            job.setStatus(JobStatus.FILLED);
        }
        jobRepository.save(job);

        // Notify student that application is accepted & contact details unlocked
        User studentUser = application.getStudent().getUser();
        notificationService.sendNotification(
                studentUser,
                "Application Accepted: " + job.getTitle(),
                "Congratulations! " + job.getOwner().getCateringName() + " has accepted your application for '" +
                        job.getTitle() + "'. Detailed location and contact information are now unlocked on your dashboard.",
                NotificationType.APPLICATION_ACCEPTED,
                application.getId()
        );

        auditService.logAction(user.getId(), "ACCEPT_APPLICANT", "JobApplication", application.getId(),
                "Accepted student " + studentUser.getFullName() + " for job: " + job.getTitle(), null);

        return dtoMapper.toApplicationResponseDto(application);
    }

    @Transactional
    public ApplicationResponseDto rejectApplicant(Long applicationId) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        CateringJob job = application.getJob();
        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to reject applicants for this job.");
        }

        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            // If already accepted, reduce workersSelected count
            if (job.getWorkersSelected() > 0) {
                job.setWorkersSelected(job.getWorkersSelected() - 1);
                if (job.getStatus() == JobStatus.FILLED) {
                    job.setStatus(JobStatus.OPEN);
                }
                jobRepository.save(job);
            }
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRespondedAt(LocalDateTime.now());
        application = applicationRepository.save(application);

        // Notify student
        User studentUser = application.getStudent().getUser();
        notificationService.sendNotification(
                studentUser,
                "Application Update: " + job.getTitle(),
                "Your application for '" + job.getTitle() + "' at " + job.getOwner().getCateringName() + " was not selected this time.",
                NotificationType.APPLICATION_REJECTED,
                application.getId()
        );

        auditService.logAction(user.getId(), "REJECT_APPLICANT", "JobApplication", application.getId(),
                "Rejected student " + studentUser.getFullName() + " for job: " + job.getTitle(), null);

        return dtoMapper.toApplicationResponseDto(application);
    }

    @Transactional
    public ApplicationResponseDto markAttendance(Long applicationId, AttendanceUpdateRequest request) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        CateringJob job = application.getJob();
        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to mark attendance for this application.");
        }

        application.setAttendanceStatus(request.getAttendanceStatus());
        if (request.getWorkCompletionStatus() != null) {
            application.setWorkCompletionStatus(request.getWorkCompletionStatus());
        }

        application = applicationRepository.save(application);

        auditService.logAction(user.getId(), "MARK_ATTENDANCE", "JobApplication", application.getId(),
                "Marked attendance as " + request.getAttendanceStatus() + " for application ID: " + application.getId(), null);

        return dtoMapper.toApplicationResponseDto(application);
    }

    @Transactional
    public JobDetailDto markJobCompleted(Long jobId) {
        User user = securityUtils.getCurrentUser();
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to complete this job.");
        }

        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);

        // Update accepted applications to COMPLETED and update student stats
        List<JobApplication> applications = applicationRepository.findByJobIdAndStatus(jobId, ApplicationStatus.ACCEPTED);
        for (JobApplication app : applications) {
            app.setStatus(ApplicationStatus.COMPLETED);
            app.setWorkCompletionStatus("COMPLETED");
            if (app.getAttendanceStatus() == AttendanceStatus.NOT_MARKED) {
                app.setAttendanceStatus(AttendanceStatus.PRESENT);
            }
            applicationRepository.save(app);

            StudentProfile student = app.getStudent();
            if (student != null) {
                student.setTotalJobsCompleted(student.getTotalJobsCompleted() + 1);
                studentProfileRepository.save(student);

                if (student.getUser() != null) {
                    notificationService.sendNotification(
                            student.getUser(),
                            "Job Completed: " + job.getTitle(),
                            "The organizer has marked '" + job.getTitle() + "' as completed. Please verify your payment once received.",
                            NotificationType.JOB_COMPLETED,
                            job.getId()
                    );
                }
            }
        }

        auditService.logAction(user.getId(), "COMPLETE_JOB", "CateringJob", job.getId(), "Marked job completed: " + job.getTitle(), null);

        return dtoMapper.toJobDetailDto(job, user, null);
    }

    @Transactional
    public PaymentResponseDto markPaymentPaid(Long applicationId, String notes) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        CateringJob job = application.getJob();
        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to update payment for this application.");
        }

        application.setPaymentStatus(PaymentStatus.PAID);
        applicationRepository.save(application);

        // Create or update PaymentRecord
        PaymentRecord paymentRecord = paymentRecordRepository.findByApplicationId(application.getId())
                .orElse(PaymentRecord.builder()
                        .application(application)
                        .job(job)
                        .student(application.getStudent())
                        .owner(job.getOwner())
                        .amount(application.getPaymentAmount())
                        .paymentType(job.getPaymentType())
                        .build());

        paymentRecord.setPaymentStatus(PaymentStatus.PAID);
        paymentRecord.setMarkedPaidAt(LocalDateTime.now());
        if (notes != null) {
            paymentRecord.setNotes(notes);
        }
        paymentRecord = paymentRecordRepository.save(paymentRecord);

        // Notify student to confirm on-spot payment
        User studentUser = application.getStudent().getUser();
        notificationService.sendNotification(
                studentUser,
                "Payment Marked as Paid: ₹" + application.getPaymentAmount(),
                "The owner marked your payment for '" + job.getTitle() + "' as PAID. Please confirm receipt on your dashboard or raise a dispute if unpaid.",
                NotificationType.PAYMENT_PAID,
                application.getId()
        );

        auditService.logAction(user.getId(), "MARK_PAYMENT_PAID", "PaymentRecord", paymentRecord.getId(),
                "Owner marked payment of ₹" + application.getPaymentAmount() + " as PAID for application ID: " + application.getId(), null);

        return dtoMapper.toPaymentResponseDto(paymentRecord);
    }
}
