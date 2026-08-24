package com.parttimejob.service;

import com.parttimejob.dto.job.JobCreateRequest;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.job.JobPublicDto;
import com.parttimejob.dto.job.JobSearchCriteria;
import com.parttimejob.dto.job.JobUpdateRequest;
import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.OwnerProfile;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.CateringJobRepository;
import com.parttimejob.repository.JobApplicationRepository;
import com.parttimejob.repository.OwnerProfileRepository;
import com.parttimejob.repository.StudentProfileRepository;
import com.parttimejob.util.DateValidationUtils;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final CateringJobRepository jobRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public JobDetailDto createJob(JobCreateRequest request) {
        User user = securityUtils.getCurrentUser();

        if (user.getRole() != Role.ROLE_OWNER && user.getRole() != Role.ROLE_ADMIN) {
            throw new ForbiddenException("Only catering owners can create job postings.");
        }

        if (user.isSuspended()) {
            throw new ForbiddenException("Your account is suspended. You cannot post jobs.");
        }

        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Owner profile not found. Please complete your profile."));

        DateValidationUtils.validateJobSchedule(request.getJobDate(), request.getStartTime(), request.getEndTime());

        CateringJob job = CateringJob.builder()
                .owner(owner)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .workType(request.getWorkType())
                .workArea(request.getWorkArea().trim())
                .detailedLocation(request.getDetailedLocation().trim())
                .jobDate(request.getJobDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .paymentAmount(request.getPaymentAmount())
                .paymentType(request.getPaymentType())
                .onSpotPayment(request.isOnSpotPayment())
                .workersRequired(request.getWorkersRequired())
                .workersSelected(0)
                .requiredSkills(request.getRequiredSkills() != null ? request.getRequiredSkills().trim() : null)
                .contactPhone(request.getContactPhone().trim())
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail().trim() : user.getEmail())
                .status(JobStatus.OPEN)
                .build();

        job = jobRepository.save(job);

        // Increment owner's total posted jobs
        owner.setTotalJobsPosted(owner.getTotalJobsPosted() + 1);
        ownerProfileRepository.save(owner);

        auditService.logAction(user.getId(), "CREATE_JOB", "CateringJob", job.getId(), "Created job: " + job.getTitle(), null);

        return dtoMapper.toJobDetailDto(job, user, null);
    }

    @Transactional
    public JobDetailDto updateJob(Long jobId, JobUpdateRequest request) {
        User user = securityUtils.getCurrentUser();
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to modify this job.");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Completed jobs cannot be edited.");
        }

        if (job.getStatus() == JobStatus.CANCELLED) {
            throw new BadRequestException("Cancelled jobs cannot be edited.");
        }

        DateValidationUtils.validateJobSchedule(request.getJobDate(), request.getStartTime(), request.getEndTime());

        if (request.getWorkersRequired() < job.getWorkersSelected()) {
            throw new BadRequestException("Workers required cannot be less than already selected workers (" + job.getWorkersSelected() + ").");
        }

        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        job.setWorkType(request.getWorkType());
        job.setWorkArea(request.getWorkArea().trim());
        job.setDetailedLocation(request.getDetailedLocation().trim());
        job.setJobDate(request.getJobDate());
        job.setStartTime(request.getStartTime());
        job.setEndTime(request.getEndTime());
        job.setPaymentAmount(request.getPaymentAmount());
        job.setPaymentType(request.getPaymentType());
        job.setOnSpotPayment(request.isOnSpotPayment());
        job.setWorkersRequired(request.getWorkersRequired());
        job.setRequiredSkills(request.getRequiredSkills() != null ? request.getRequiredSkills().trim() : null);
        job.setContactPhone(request.getContactPhone().trim());
        job.setContactEmail(request.getContactEmail() != null ? request.getContactEmail().trim() : null);

        // Check if status should update
        if (job.getWorkersSelected() >= job.getWorkersRequired()) {
            job.setStatus(JobStatus.FILLED);
        } else if (job.getStatus() == JobStatus.FILLED) {
            job.setStatus(JobStatus.OPEN);
        }

        job = jobRepository.save(job);
        auditService.logAction(user.getId(), "UPDATE_JOB", "CateringJob", job.getId(), "Updated job: " + job.getTitle(), null);

        return dtoMapper.toJobDetailDto(job, user, null);
    }

    @Transactional
    public void cancelJob(Long jobId) {
        User user = securityUtils.getCurrentUser();
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        boolean isOwner = job.getOwner().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to cancel this job.");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Completed jobs cannot be cancelled.");
        }

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);

        // Notify all applied & accepted students
        List<JobApplication> applications = applicationRepository.findByJobIdOrderByAppliedAtAsc(jobId);
        for (JobApplication app : applications) {
            if (app.getStatus() == ApplicationStatus.APPLIED || app.getStatus() == ApplicationStatus.ACCEPTED) {
                app.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(app);

                if (app.getStudent() != null && app.getStudent().getUser() != null) {
                    notificationService.sendNotification(
                            app.getStudent().getUser(),
                            "Job Cancelled: " + job.getTitle(),
                            "The catering job '" + job.getTitle() + "' scheduled on " + job.getJobDate() + " has been cancelled by the organizer.",
                            NotificationType.JOB_CANCELLED,
                            job.getId()
                    );
                }
            }
        }

        auditService.logAction(user.getId(), "CANCEL_JOB", "CateringJob", job.getId(), "Cancelled job: " + job.getTitle(), null);
    }

    @Transactional(readOnly = true)
    public JobDetailDto getJobById(Long jobId) {
        CateringJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        User currentUser = null;
        Long currentStudentProfileId = null;

        Optional<String> currentUserEmail = securityUtils.getCurrentUserEmail();
        if (currentUserEmail.isPresent()) {
            currentUser = securityUtils.getCurrentUser();
            if (currentUser.getRole() == Role.ROLE_STUDENT && currentUser.getStudentProfile() != null) {
                currentStudentProfileId = currentUser.getStudentProfile().getId();
            }
        }

        return dtoMapper.toJobDetailDto(job, currentUser, currentStudentProfileId);
    }

    @Transactional(readOnly = true)
    public List<JobPublicDto> searchJobs(JobSearchCriteria criteria) {
        Long currentStudentProfileId = null;
        Optional<String> currentUserEmail = securityUtils.getCurrentUserEmail();
        if (currentUserEmail.isPresent()) {
            try {
                User user = securityUtils.getCurrentUser();
                if (user.getRole() == Role.ROLE_STUDENT && user.getStudentProfile() != null) {
                    currentStudentProfileId = user.getStudentProfile().getId();
                }
            } catch (Exception ignored) {}
        }

        List<CateringJob> jobs = jobRepository.searchJobs(
                JobStatus.OPEN,
                (criteria.getArea() != null && !criteria.getArea().trim().isEmpty()) ? criteria.getArea().trim() : null,
                (criteria.getLocation() != null && !criteria.getLocation().trim().isEmpty()) ? criteria.getLocation().trim() : null,
                criteria.getWorkType(),
                criteria.getJobDate(),
                criteria.getMinPayment(),
                criteria.getMaxPayment(),
                criteria.getPaymentType(),
                criteria.getStartTime(),
                criteria.getEndTime()
        );

        final Long studentId = currentStudentProfileId;
        return jobs.stream()
                .map(job -> dtoMapper.toJobPublicDto(job, studentId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobPublicDto> getRecommendedJobs() {
        String preferredArea = null;
        Long currentStudentProfileId = null;

        Optional<String> currentUserEmail = securityUtils.getCurrentUserEmail();
        if (currentUserEmail.isPresent()) {
            try {
                User user = securityUtils.getCurrentUser();
                if (user.getRole() == Role.ROLE_STUDENT && user.getStudentProfile() != null) {
                    currentStudentProfileId = user.getStudentProfile().getId();
                    preferredArea = user.getStudentProfile().getPreferredArea();
                }
            } catch (Exception ignored) {}
        }

        List<CateringJob> jobs = jobRepository.findRecommendedJobs(preferredArea);
        final Long studentId = currentStudentProfileId;
        return jobs.stream()
                .map(job -> dtoMapper.toJobPublicDto(job, studentId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobPublicDto> getAllOpenJobs() {
        return searchJobs(new JobSearchCriteria());
    }
}
