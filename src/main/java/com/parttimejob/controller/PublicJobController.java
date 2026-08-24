package com.parttimejob.controller;

import com.parttimejob.dto.common.ApiResponse;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.job.JobPublicDto;
import com.parttimejob.dto.job.JobSearchCriteria;
import com.parttimejob.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobPublicDto>>> searchJobs(JobSearchCriteria criteria) {
        List<JobPublicDto> jobs = jobService.searchJobs(criteria);
        return ResponseEntity.ok(ApiResponse.ok("Jobs retrieved successfully", jobs));
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<JobPublicDto>>> getRecommendedJobs() {
        List<JobPublicDto> jobs = jobService.getRecommendedJobs();
        return ResponseEntity.ok(ApiResponse.ok("Recommended jobs retrieved", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDetailDto>> getJobDetails(@PathVariable Long id) {
        JobDetailDto job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.ok("Job details retrieved", job));
    }
}
