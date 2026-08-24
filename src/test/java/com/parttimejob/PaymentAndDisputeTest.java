package com.parttimejob;

import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.dto.report.ReportCreateRequest;
import com.parttimejob.dto.report.ReportResolveRequest;
import com.parttimejob.dto.report.ReportResponseDto;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.enums.PaymentStatus;
import com.parttimejob.enums.ReportStatus;
import com.parttimejob.enums.ReportType;
import com.parttimejob.repository.JobApplicationRepository;
import com.parttimejob.service.AdminService;
import com.parttimejob.service.OwnerService;
import com.parttimejob.service.PaymentService;
import com.parttimejob.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class PaymentAndDisputeTest {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private JobApplicationRepository applicationRepository;

    private void authenticate(String email, String role) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        email,
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully execute On-Spot Payment lifecycle: Owner marks PAID -> Student CONFIRMS")
    void testPaymentLifecycle() {
        // Find accepted application for Pavan
        List<JobApplication> apps = applicationRepository.findAll();
        JobApplication acceptedApp = apps.stream()
                .filter(a -> a.getStudent().getUser().getEmail().equals("student.pavan@gmail.com") &&
                        a.getStatus() == com.parttimejob.enums.ApplicationStatus.ACCEPTED)
                .findFirst()
                .orElseThrow();

        // 1. Owner marks PAID
        authenticate("owner.srilakshmi@catering.com", "ROLE_OWNER");
        PaymentResponseDto pay = ownerService.markPaymentPaid(acceptedApp.getId(), "Paid cash in hand at 11:15 PM");
        assertNotNull(pay);
        assertEquals(PaymentStatus.PAID, pay.getPaymentStatus());

        // 2. Student confirms receipt
        authenticate("student.pavan@gmail.com", "ROLE_STUDENT");
        PaymentResponseDto confirmed = paymentService.confirmPaymentReceived(acceptedApp.getId(), "Received full ₹750");
        assertNotNull(confirmed);
        assertEquals(PaymentStatus.CONFIRMED, confirmed.getPaymentStatus());
        assertNotNull(confirmed.getConfirmedPaidAt());
    }

    @Test
    @DisplayName("Should file a payment dispute and allow admin to resolve it")
    void testDisputeAndResolution() {
        List<JobApplication> apps = applicationRepository.findAll();
        JobApplication app = apps.get(0);

        // 1. Student files dispute
        authenticate("student.pavan@gmail.com", "ROLE_STUDENT");
        ReportCreateRequest reportReq = ReportCreateRequest.builder()
                .jobId(app.getJob().getId())
                .applicationId(app.getId())
                .reportType(ReportType.PAYMENT_PARTIALLY_RECEIVED)
                .description("Promised 750 but paid only 500")
                .expectedAmount(new BigDecimal("750.00"))
                .receivedAmount(new BigDecimal("500.00"))
                .evidenceNotes("Cash receipt photo attached")
                .build();

        ReportResponseDto report = reportService.createReport(reportReq);
        assertNotNull(report);
        assertEquals(ReportStatus.PENDING, report.getStatus());

        // 2. Admin resolves dispute
        authenticate("admin@parttimejob.com", "ROLE_ADMIN");
        ReportResolveRequest resolveReq = ReportResolveRequest.builder()
                .status(ReportStatus.RESOLVED)
                .adminRemarks("Owner instructed to transfer remaining ₹250. Owner complied.")
                .build();

        ReportResponseDto resolved = reportService.resolveReport(report.getId(), resolveReq);
        assertNotNull(resolved);
        assertEquals(ReportStatus.RESOLVED, resolved.getStatus());
        assertEquals("Owner instructed to transfer remaining ₹250. Owner complied.", resolved.getAdminRemarks());
    }
}
