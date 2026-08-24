package com.parttimejob.service;

import com.parttimejob.dto.application.ApplicationRequest;
import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.AttendanceStatus;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ConflictException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.CateringJobRepository;
import com.parttimejob.repository.JobApplicationRepository;
import com.parttimejob.repository.StudentProfileRepository;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final CateringJobRepository jobRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public ApplicationResponseDto applyForJob(Long jobId, ApplicationRequest request) {
        User user = securityUtils.getCurrentUser();

        if (user.getRole() != Role.ROLE_STUDENT && user.getRole() != Role.ROLE_ADMIN) {
            throw new ForbiddenException("Only students can apply for catering jobs.");
        }

        if (user.isSuspended()) {
            throw new ForbiddenException("Your account is currently suspended. You cannot apply for jobs.");
        }

        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Student profile not found. Please complete your profile."));

        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        if (job.getStatus() == JobStatus.CANCELLED) {
            throw new BadRequestException("This job has been cancelled and is no longer accepting applications.");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("This job has already been completed.");
        }

        if (applicationRepository.existsByJobIdAndStudentId(job.getId(), student.getId())) {
            throw new ConflictException("You have already applied for this job.");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .student(student)
                .status(ApplicationStatus.APPLIED)
                .attendanceStatus(AttendanceStatus.NOT_MARKED)
                .workCompletionStatus("NOT_COMPLETED")
                .paymentStatus(PaymentStatus.PENDING)
                .paymentAmount(job.getPaymentAmount())
                .notes(request != null ? request.getNotes() : null)
                .build();

        application = applicationRepository.save(application);

        // Notify job owner
        if (job.getOwner() != null && job.getOwner().getUser() != null) {
            notificationService.sendNotification(
                    job.getOwner().getUser(),
                    "New Application: " + job.getTitle(),
                    student.getUser().getFullName() + " applied for '" + job.getTitle() + "' in " + job.getWorkArea() + ".",
                    NotificationType.APPLICATION_RECEIVED,
                    application.getId()
            );
        }

        auditService.logAction(user.getId(), "APPLY_JOB", "JobApplication", application.getId(),
                "Applied for job: " + job.getTitle(), null);

        return dtoMapper.toApplicationResponseDto(application);
    }

    @Transactional
    public void cancelApplication(Long applicationId) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        boolean isStudent = application.getStudent().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isStudent && !isAdmin) {
            throw new ForbiddenException("You are not authorized to cancel this application.");
        }

        if (application.getStatus() != ApplicationStatus.APPLIED) {
            throw new BadRequestException("Only pending applications in 'APPLIED' status can be cancelled. Current status: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.CANCELLED);
        applicationRepository.save(application);

        // Notify owner
        CateringJob job = application.getJob();
        if (job != null && job.getOwner() != null && job.getOwner().getUser() != null) {
            notificationService.sendNotification(
                    job.getOwner().getUser(),
                    "Application Cancelled",
                    application.getStudent().getUser().getFullName() + " withdrew their application for '" + job.getTitle() + "'.",
                    NotificationType.APPLICATION_CANCELLED,
                    application.getId()
            );
        }

        auditService.logAction(user.getId(), "CANCEL_APPLICATION", "JobApplication", application.getId(),
                "Cancelled application for job: " + (job != null ? job.getTitle() : "N/A"), null);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getMyApplications() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Student profile not found."));

        return applicationRepository.findByStudentIdOrderByAppliedAtDesc(student.getId())
                .stream()
                .map(dtoMapper::toApplicationResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getMyAcceptedJobs() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Student profile not found."));

        return applicationRepository.findByStudentIdAndStatusOrderByAppliedAtDesc(student.getId(), ApplicationStatus.ACCEPTED)
                .stream()
                .map(dtoMapper::toApplicationResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getMyCompletedJobs() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Student profile not found."));

        return applicationRepository.findByStudentIdAndStatusOrderByAppliedAtDesc(student.getId(), ApplicationStatus.COMPLETED)
                .stream()
                .map(dtoMapper::toApplicationResponseDto)
                .collect(Collectors.toList());
    }
}
