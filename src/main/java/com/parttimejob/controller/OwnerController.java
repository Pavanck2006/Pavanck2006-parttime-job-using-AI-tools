package com.parttimejob.controller;

import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.dto.application.AttendanceUpdateRequest;
import com.parttimejob.dto.common.ApiResponse;
import com.parttimejob.dto.job.JobCreateRequest;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.job.JobUpdateRequest;
import com.parttimejob.dto.payment.PaymentActionRequest;
import com.parttimejob.dto.payment.PaymentResponseDto;
import com.parttimejob.dto.user.OwnerProfileDto;
import com.parttimejob.dto.user.ProfileUpdateRequest;
import com.parttimejob.service.JobService;
import com.parttimejob.service.OwnerService;
import com.parttimejob.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final JobService jobService;
    private final PaymentService paymentService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<OwnerProfileDto>> getProfile() {
        OwnerProfileDto profile = ownerService.getOwnerProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<OwnerProfileDto>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        OwnerProfileDto profile = ownerService.updateOwnerProfile(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<JobDetailDto>> createJob(@Valid @RequestBody JobCreateRequest request) {
        JobDetailDto job = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Job posted successfully!", job));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDetailDto>>> getMyJobs() {
        List<JobDetailDto> jobs = ownerService.getMyJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobDetailDto>> getJob(@PathVariable Long id) {
        JobDetailDto job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobDetailDto>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request
    ) {
        JobDetailDto job = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Job updated successfully", job));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelJob(@PathVariable Long id) {
        jobService.cancelJob(id);
        return ResponseEntity.ok(ApiResponse.ok("Job cancelled successfully", null));
    }

    @GetMapping("/jobs/{id}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponseDto>>> getJobApplications(@PathVariable Long id) {
        List<ApplicationResponseDto> apps = ownerService.getJobApplicants(id);
        return ResponseEntity.ok(ApiResponse.ok(apps));
    }

    @PutMapping("/applications/{id}/accept")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> acceptApplicant(@PathVariable Long id) {
        ApplicationResponseDto app = ownerService.acceptApplicant(id);
        return ResponseEntity.ok(ApiResponse.ok("Applicant accepted successfully! Worker limit updated.", app));
    }

    @PutMapping("/applications/{id}/reject")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> rejectApplicant(@PathVariable Long id) {
        ApplicationResponseDto app = ownerService.rejectApplicant(id);
        return ResponseEntity.ok(ApiResponse.ok("Applicant rejected.", app));
    }

    @PutMapping("/applications/{id}/attendance")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> markAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceUpdateRequest request
    ) {
        ApplicationResponseDto app = ownerService.markAttendance(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Attendance updated successfully", app));
    }

    @PutMapping("/jobs/{id}/complete")
    public ResponseEntity<ApiResponse<JobDetailDto>> completeJob(@PathVariable Long id) {
        JobDetailDto job = ownerService.markJobCompleted(id);
        return ResponseEntity.ok(ApiResponse.ok("Job marked as completed!", job));
    }

    @PutMapping("/applications/{id}/payment")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> markPaymentPaid(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentActionRequest request
    ) {
        String notes = (request != null) ? request.getNotes() : null;
        PaymentResponseDto payment = ownerService.markPaymentPaid(id, notes);
        return ResponseEntity.ok(ApiResponse.ok("Payment marked as PAID successfully!", payment));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getMyPayments() {
        List<PaymentResponseDto> payments = paymentService.getOwnerPayments();
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }
}
