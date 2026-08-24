package com.parttimejob.dto.auth;

import com.parttimejob.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private boolean active;
    private boolean suspended;
    private LocalDateTime createdAt;
}
