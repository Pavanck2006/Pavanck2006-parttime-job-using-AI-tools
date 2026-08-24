package com.parttimejob.service;

import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.PaymentRecord;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.JobApplicationRepository;
import com.parttimejob.repository.OwnerProfileRepository;
import com.parttimejob.repository.PaymentRecordRepository;
import com.parttimejob.repository.StudentProfileRepository;
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
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final JobApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public PaymentResponseDto confirmPaymentReceived(Long applicationId, String notes) {
        User user = securityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        boolean isStudent = application.getStudent().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isStudent && !isAdmin) {
            throw new ForbiddenException("Only the accepted student can confirm receipt of payment.");
        }

        application.setPaymentStatus(PaymentStatus.CONFIRMED);
        application.setPaymentConfirmationDate(LocalDateTime.now());
        applicationRepository.save(application);

        PaymentRecord paymentRecord = paymentRecordRepository.findByApplicationId(application.getId())
                .orElse(PaymentRecord.builder()
                        .application(application)
                        .job(application.getJob())
                        .student(application.getStudent())
                        .owner(application.getJob().getOwner())
                        .amount(application.getPaymentAmount())
                        .paymentType(application.getJob().getPaymentType())
                        .markedPaidAt(LocalDateTime.now())
                        .build());

        paymentRecord.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRecord.setConfirmedPaidAt(LocalDateTime.now());
        if (notes != null) {
            paymentRecord.setNotes(notes);
        }
        paymentRecord = paymentRecordRepository.save(paymentRecord);

        // Notify owner
        if (application.getJob() != null && application.getJob().getOwner() != null &&
                application.getJob().getOwner().getUser() != null) {
            notificationService.sendNotification(
                    application.getJob().getOwner().getUser(),
                    "Payment Confirmed by Student",
                    application.getStudent().getUser().getFullName() + " confirmed receipt of ₹" +
                            application.getPaymentAmount() + " for '" + application.getJob().getTitle() + "'.",
                    NotificationType.PAYMENT_CONFIRMED,
                    application.getId()
            );
        }

        auditService.logAction(user.getId(), "CONFIRM_PAYMENT", "PaymentRecord", paymentRecord.getId(),
                "Student confirmed payment of ₹" + application.getPaymentAmount(), null);

        return dtoMapper.toPaymentResponseDto(paymentRecord);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getStudentPayments() {
        User user = securityUtils.getCurrentUser();
        StudentProfile student = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Student profile not found."));

        return paymentRecordRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(dtoMapper::toPaymentResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getOwnerPayments() {
        User user = securityUtils.getCurrentUser();
        var owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Owner profile not found."));

        return paymentRecordRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(dtoMapper::toPaymentResponseDto)
                .collect(Collectors.toList());
    }
}
