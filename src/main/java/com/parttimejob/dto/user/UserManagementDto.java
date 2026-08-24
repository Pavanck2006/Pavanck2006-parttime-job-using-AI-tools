package com.parttimejob.dto.user;

import com.parttimejob.enums.Role;
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
public class UserManagementDto {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private boolean active;
    private boolean suspended;
    private LocalDateTime createdAt;
    
    // Additional role-specific fields
    private Long profileId;
    private String collegeName;
    private String cateringName;
    private VerificationStatus verificationStatus;
    private int jobsCount; // completed for student, posted for owner
}
