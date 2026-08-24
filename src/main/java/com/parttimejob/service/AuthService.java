package com.parttimejob.service;

import com.parttimejob.config.JwtService;
import com.parttimejob.dto.auth.AuthResponse;
import com.parttimejob.dto.auth.LoginRequest;
import com.parttimejob.dto.auth.RegisterRequest;
import com.parttimejob.entity.OwnerProfile;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.Role;
import com.parttimejob.enums.VerificationStatus;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ConflictException;
import com.parttimejob.exception.ForbiddenException;
import com.parttimejob.exception.UnauthorizedException;
import com.parttimejob.repository.OwnerProfileRepository;
import com.parttimejob.repository.StudentProfileRepository;
import com.parttimejob.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with email " + email + " already exists.");
        }

        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new BadRequestException("Direct registration as Administrator is not permitted.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .role(request.getRole())
                .active(true)
                .suspended(false)
                .build();

        user = userRepository.save(user);

        Long profileId = null;
        String cateringName = null;
        String verificationStatus = null;
        String preferredArea = null;

        if (request.getRole() == Role.ROLE_STUDENT) {
            StudentProfile studentProfile = StudentProfile.builder()
                    .user(user)
                    .collegeName(request.getCollegeName() != null ? request.getCollegeName().trim() : null)
                    .preferredArea(request.getPreferredArea() != null ? request.getPreferredArea().trim() : null)
                    .skills(request.getSkills() != null ? request.getSkills().trim() : null)
                    .bio(request.getBio() != null ? request.getBio().trim() : null)
                    .emergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact().trim() : null)
                    .totalJobsCompleted(0)
                    .rating(5.0)
                    .build();
            studentProfile = studentProfileRepository.save(studentProfile);
            profileId = studentProfile.getId();
            preferredArea = studentProfile.getPreferredArea();
        } else if (request.getRole() == Role.ROLE_OWNER) {
            String catName = request.getCateringName();
            if (catName == null || catName.trim().isEmpty()) {
                catName = request.getFullName().trim() + " Catering";
            }
            OwnerProfile ownerProfile = OwnerProfile.builder()
                    .user(user)
                    .cateringName(catName.trim())
                    .businessAddress(request.getBusinessAddress() != null ? request.getBusinessAddress().trim() : null)
                    .businessPhone(request.getBusinessPhone() != null ? request.getBusinessPhone().trim() : request.getPhone())
                    .verificationStatus(VerificationStatus.PENDING_VERIFICATION)
                    .totalJobsPosted(0)
                    .build();
            ownerProfile = ownerProfileRepository.save(ownerProfile);
            profileId = ownerProfile.getId();
            cateringName = ownerProfile.getCateringName();
            verificationStatus = ownerProfile.getVerificationStatus().name();
        }

        String token = jwtService.generateToken(user);
        auditService.logAction(user.getId(), "REGISTER", "User", user.getId(), "User registered as " + user.getRole(), null);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .suspended(user.isSuspended())
                .profileId(profileId)
                .cateringName(cateringName)
                .verificationStatus(verificationStatus)
                .preferredArea(preferredArea)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (user.isSuspended()) {
            throw new ForbiddenException("Your account has been suspended by the platform administrator. Please contact support.");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("Your account is deactivated. Please contact support.");
        }

        Long profileId = null;
        String cateringName = null;
        String verificationStatus = null;
        String preferredArea = null;

        if (user.getRole() == Role.ROLE_STUDENT && user.getStudentProfile() != null) {
            profileId = user.getStudentProfile().getId();
            preferredArea = user.getStudentProfile().getPreferredArea();
        } else if (user.getRole() == Role.ROLE_OWNER && user.getOwnerProfile() != null) {
            profileId = user.getOwnerProfile().getId();
            cateringName = user.getOwnerProfile().getCateringName();
            verificationStatus = user.getOwnerProfile().getVerificationStatus().name();
        }

        String token = jwtService.generateToken(user);
        auditService.logAction(user.getId(), "LOGIN", "User", user.getId(), "User logged in", null);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .suspended(user.isSuspended())
                .profileId(profileId)
                .cateringName(cateringName)
                .verificationStatus(verificationStatus)
                .preferredArea(preferredArea)
                .build();
    }
}
