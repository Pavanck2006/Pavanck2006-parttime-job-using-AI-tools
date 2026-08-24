package com.parttimejob.dto.auth;

import com.parttimejob.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone must be a valid 10-15 digit number")
    private String phone;

    @NotNull(message = "Role is required (ROLE_STUDENT or ROLE_OWNER)")
    private Role role;

    // Student-specific fields (Optional during initial registration, can be filled in profile)
    private String collegeName;
    private String preferredArea;
    private String skills;
    private String bio;
    private String emergencyContact;

    // Owner-specific fields
    private String cateringName;
    private String businessAddress;
    private String businessPhone;
}
