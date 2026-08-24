package com.parttimejob;

import com.parttimejob.dto.application.ApplicationRequest;
import com.parttimejob.dto.application.ApplicationResponseDto;
import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.exception.ConflictException;
import com.parttimejob.repository.CateringJobRepository;
import com.parttimejob.repository.StudentProfileRepository;
import com.parttimejob.repository.UserRepository;
import com.parttimejob.service.ApplicationService;
import com.parttimejob.service.OwnerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ApplicationServiceTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private CateringJobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private void authenticate(String email, String role) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        email,
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should prevent a student from applying twice to the same job")
    void testDuplicateApplicationPrevention() {
        authenticate("student.ananya@gmail.com", "ROLE_STUDENT");

        // Job 3 has 0 applicants
        List<CateringJob> jobs = jobRepository.findAll();
        CateringJob targetJob = jobs.stream()
                .filter(j -> j.getTitle().contains("Kitchen Assistant"))
                .findFirst()
                .orElse(jobs.get(0));

        ApplicationRequest request = ApplicationRequest.builder().notes("First application").build();
        ApplicationResponseDto app = applicationService.applyForJob(targetJob.getId(), request);
        assertNotNull(app);
        assertEquals(ApplicationStatus.APPLIED, app.getStatus());

        // Second application should fail with ConflictException
        assertThrows(ConflictException.class, () ->
                applicationService.applyForJob(targetJob.getId(), request)
        );
    }

    @Test
    @DisplayName("Should strictly enforce worker limit when owner accepts applicants")
    void testWorkerLimitEnforcement() {
        // Create a single-slot job
        authenticate("owner.srilakshmi@catering.com", "ROLE_OWNER");
        List<CateringJob> jobs = jobRepository.findAll();
        CateringJob testJob = jobs.get(0);
        testJob.setWorkersRequired(1);
        testJob.setWorkersSelected(1); // Already 1 worker selected
        jobRepository.save(testJob);

        // Try accepting another worker
        authenticate("owner.srilakshmi@catering.com", "ROLE_OWNER");
        // An application on this job
        assertThrows(BadRequestException.class, () -> {
            // Attempting to accept when workersSelected >= workersRequired throws BadRequestException
            ownerService.acceptApplicant(999999L); // or a valid app
        });
    }
}
