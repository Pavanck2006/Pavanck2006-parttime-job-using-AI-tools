package com.parttimejob.service;

import com.parttimejob.dto.common.DashboardStatsDto;
import com.parttimejob.dto.user.ProfileUpdateRequest;
import com.parttimejob.dto.user.StudentProfileDto;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.PaymentRecord;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.*;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final CateringJobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public StudentProfileDto getProfile() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));
        return dtoMapper.toStudentProfileDto(student);
    }

    @Transactional
    public StudentProfileDto updateProfile(ProfileUpdateRequest request) {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getCollegeName() != null) {
            student.setCollegeName(request.getCollegeName().trim());
        }
        if (request.getPreferredArea() != null) {
            student.setPreferredArea(request.getPreferredArea().trim());
        }
        if (request.getSkills() != null) {
            student.setSkills(request.getSkills().trim());
        }
        if (request.getBio() != null) {
            student.setBio(request.getBio().trim());
        }
        if (request.getEmergencyContact() != null) {
            student.setEmergencyContact(request.getEmergencyContact().trim());
        }

        studentProfileRepository.save(student);
        auditService.logAction(user.getId(), "UPDATE_STUDENT_PROFILE", "StudentProfile", student.getId(), "Updated student profile", null);

        return dtoMapper.toStudentProfileDto(student);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        long availableJobs = jobRepository.countByStatus(JobStatus.OPEN);
        List<JobApplication> myApps = applicationRepository.findByStudentIdOrderByAppliedAtDesc(student.getId());

        long appliedCount = myApps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPLIED).count();
        long acceptedCount = myApps.stream().filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).count();
        long completedCount = myApps.stream().filter(a -> a.getStatus() == ApplicationStatus.COMPLETED).count();
        long pendingPayments = myApps.stream().filter(a -> a.getPaymentStatus() == PaymentStatus.PENDING &&
                (a.getStatus() == ApplicationStatus.ACCEPTED || a.getStatus() == ApplicationStatus.COMPLETED)).count();

        List<PaymentRecord> payments = paymentRecordRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        BigDecimal totalEarnings = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID || p.getPaymentStatus() == PaymentStatus.CONFIRMED)
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardStatsDto.builder()
                .availableJobsCount(availableJobs)
                .appliedJobsCount(appliedCount)
                .acceptedJobsCount(acceptedCount)
                .completedJobsCount(completedCount)
                .pendingPaymentsCount(pendingPayments)
                .totalEarnings(totalEarnings)
                .build();
    }
}
