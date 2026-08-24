package com.parttimejob.dto.user;

import com.parttimejob.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProfileDto {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String cateringName;
    private String businessAddress;
    private String businessPhone;
    private VerificationStatus verificationStatus;
    private boolean verified;
    private LocalDateTime verifiedAt;
    private int totalJobsPosted;
    private boolean active;
    private boolean suspended;
    private LocalDateTime createdAt;
}
