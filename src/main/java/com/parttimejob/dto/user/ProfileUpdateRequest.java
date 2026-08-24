package com.parttimejob.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    private String fullName;
    private String phone;
    
    // Student fields
    private String collegeName;
    private String preferredArea;
    private String skills;
    private String bio;
    private String emergencyContact;
    
    // Owner fields
    private String cateringName;
    private String businessAddress;
    private String businessPhone;
}
