package com.parttimejob.controller;

import com.parttimejob.dto.application.ApplicationRequest;
import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.dto.common.ApiResponse;
import com.parttimejob.dto.common.DashboardStatsDto;
import com.parttimejob.dto.job.JobPublicDto;
import com.parttimejob.dto.job.JobSearchCriteria;
import com.parttimejob.dto.payment.PaymentActionRequest;
import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.dto.user.ProfileUpdateRequest;
import com.parttimejob.dto.user.StudentProfileDto;
import com.parttimejob.service.ApplicationService;
import com.parttimejob.service.JobService;
import com.parttimejob.service.PaymentService;
import com.parttimejob.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final PaymentService paymentService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getProfile() {
        StudentProfileDto profile = studentService.getProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileDto>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        StudentProfileDto profile = studentService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = studentService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobPublicDto>>> searchJobs(JobSearchCriteria criteria) {
        List<JobPublicDto> jobs = jobService.searchJobs(criteria);
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> applyForJob(
            @PathVariable Long jobId,
            @RequestBody(required = false) ApplicationRequest request
    ) {
        ApplicationResponseDto app = applicationService.applyForJob(jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Application submitted successfully!", app));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponseDto>>> getMyApplications() {
        List<ApplicationResponseDto> apps = applicationService.getMyApplications();
        return ResponseEntity.ok(ApiResponse.ok(apps));
    }

    @GetMapping("/applications/accepted")
    public ResponseEntity<ApiResponse<List<ApplicationResponseDto>>> getMyAcceptedJobs() {
        List<ApplicationResponseDto> apps = applicationService.getMyAcceptedJobs();
        return ResponseEntity.ok(ApiResponse.ok(apps));
    }

    @GetMapping("/applications/completed")
    public ResponseEntity<ApiResponse<List<ApplicationResponseDto>>> getMyCompletedJobs() {
        List<ApplicationResponseDto> apps = applicationService.getMyCompletedJobs();
        return ResponseEntity.ok(ApiResponse.ok(apps));
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelApplication(@PathVariable Long id) {
        applicationService.cancelApplication(id);
        return ResponseEntity.ok(ApiResponse.ok("Application cancelled successfully", null));
    }

    @PutMapping("/applications/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentActionRequest request
    ) {
        String notes = (request != null) ? request.getNotes() : null;
        PaymentResponseDto payment = paymentService.confirmPaymentReceived(id, notes);
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed successfully!", payment));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getMyPayments() {
        List<PaymentResponseDto> payments = paymentService.getStudentPayments();
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }
}
