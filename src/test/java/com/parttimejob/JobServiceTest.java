package com.parttimejob;

import com.parttimejob.dto.job.JobCreateRequest;
import com.parttimejob.dto.job.JobDetailDto;
import com.parttimejob.dto.job.JobPublicDto;
import com.parttimejob.dto.job.JobSearchCriteria;
import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.WorkType;
import com.parttimejob.exception.BadRequestException;
import com.parttimejob.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class JobServiceTest {

    @Autowired
    private JobService jobService;

    private void authenticateAsOwner() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        "owner.srilakshmi@catering.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER"))
                );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully create a job with valid schedule and On-Spot payment")
    void testCreateJobSuccess() {
        authenticateAsOwner();

        JobCreateRequest request = JobCreateRequest.builder()
                .title("Weekend Banquet Server")
                .description("Table setup and food serving for anniversary banquet.")
                .workType(WorkType.FOOD_SERVER)
                .workArea("Kengeri")
                .detailedLocation("Sri Manjunatha Hall, Kengeri, Bangalore")
                .jobDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(22, 0))
                .paymentAmount(new BigDecimal("800.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(4)
                .requiredSkills("Uniform, food counter skills")
                .contactPhone("+919845012345")
                .contactEmail("owner.srilakshmi@catering.com")
                .build();

        JobDetailDto created = jobService.createJob(request);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Weekend Banquet Server", created.getTitle());
        assertEquals(4, created.getWorkersRequired());
        assertEquals(0, created.getWorkersSelected());
    }

    @Test
    @DisplayName("Should reject job creation with invalid time range (startTime >= endTime)")
    void testInvalidTimeRange() {
        authenticateAsOwner();

        JobCreateRequest request = JobCreateRequest.builder()
                .title("Faulty Time Job")
                .description("Description")
                .workType(WorkType.CLEANER)
                .workArea("Kengeri")
                .detailedLocation("Hall 123")
                .jobDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(18, 0)) // End time before start time
                .paymentAmount(new BigDecimal("500.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .workersRequired(2)
                .contactPhone("+919845012345")
                .build();

        assertThrows(BadRequestException.class, () -> jobService.createJob(request));
    }

    @Test
    @DisplayName("Should search and filter jobs by area")
    void testSearchJobsByArea() {
        JobSearchCriteria criteria = JobSearchCriteria.builder()
                .area("Kengeri")
                .build();

        List<JobPublicDto> results = jobService.searchJobs(criteria);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(j -> j.getWorkArea().equalsIgnoreCase("Kengeri")));
    }
}
