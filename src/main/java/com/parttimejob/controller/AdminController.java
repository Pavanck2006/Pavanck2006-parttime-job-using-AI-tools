package com.parttimejob.controller;

import com.parttimejob.dto.common.ApiResponse;
import com.parttimejob.dto.common.DashboardStatsDto;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.report.ReportResolveRequest;
import com.parttimejob.dto.report.ReportResponseDto;
import com.parttimejob.dto.user.OwnerProfileDto;
import com.parttimejob.dto.user.StudentProfileDto;
import com.parttimejob.dto.user.UserManagementDto;
import com.parttimejob.service.AdminService;
import com.parttimejob.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserManagementDto>>> getAllUsers() {
        List<UserManagementDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentProfileDto>>> getAllStudents() {
        List<StudentProfileDto> students = adminService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @GetMapping("/owners")
    public ResponseEntity<ApiResponse<List<OwnerProfileDto>>> getAllOwners() {
        List<OwnerProfileDto> owners = adminService.getAllOwners();
        return ResponseEntity.ok(ApiResponse.ok(owners));
    }

    @PutMapping("/owners/{id}/verify")
    public ResponseEntity<ApiResponse<OwnerProfileDto>> verifyOwner(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean verified
    ) {
        OwnerProfileDto owner = adminService.verifyOwner(id, verified);
        String msg = verified ? "Owner verified successfully! Verified badge granted." : "Owner verification reverted to pending.";
        return ResponseEntity.ok(ApiResponse.ok(msg, owner));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<UserManagementDto>> setUserSuspension(
            @PathVariable Long id,
            @RequestParam boolean suspended,
            @RequestParam(required = false) String reason
    ) {
        UserManagementDto user = adminService.setUserSuspension(id, suspended, reason);
        String msg = suspended ? "User suspended successfully" : "User reactivated successfully";
        return ResponseEntity.ok(ApiResponse.ok(msg, user));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDetailDto>>> getAllJobs() {
        List<JobDetailDto> jobs = adminService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        adminService.deleteJob(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Job deleted/cancelled by admin", null));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ReportResponseDto>>> getAllReports() {
        List<ReportResponseDto> reports = reportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @PutMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<ReportResponseDto>> resolveReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportResolveRequest request
    ) {
        ReportResponseDto report = reportService.resolveReport(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Report resolved successfully", report));
    }
}
