package com.parttimejob.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String collegeName;
    private String preferredArea;
    private String skills;
    private String bio;
    private String emergencyContact;
    private int totalJobsCompleted;
    private Double rating;
    private boolean active;
    private boolean suspended;
    private LocalDateTime createdAt;
}
