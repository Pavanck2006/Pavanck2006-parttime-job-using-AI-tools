package com.parttimejob.controller;

import com.parttimejob.dto.common.ApiResponse;
import com.parttimejob.dto.report.ReportCreateRequest;
import com.parttimejob.dto.report.ReportResponseDto;
import com.parttimejob.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponseDto>> createReport(@Valid @RequestBody ReportCreateRequest request) {
        ReportResponseDto report = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report submitted successfully. Platform administrators will investigate.", report));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<ApiResponse<List<ReportResponseDto>>> getMyReports() {
        List<ReportResponseDto> reports = reportService.getMyReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }
}
