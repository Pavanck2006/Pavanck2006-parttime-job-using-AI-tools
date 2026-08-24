package com.parttimejob.dto.auth;

import com.parttimejob.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private boolean active;
    private boolean suspended;
    
    // Additional role metadata
    private Long profileId;
    private String cateringName;
    private String verificationStatus;
    private String preferredArea;
}
