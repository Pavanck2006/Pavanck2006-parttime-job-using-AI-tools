package com.parttimejob.config;

import com.parttimejob.entity.*;
import com.parttimejob.enums.*;
import com.parttimejob.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final CateringJobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded with user accounts.");
            return;
        }

        log.info("Seeding database with initial users, jobs, applications, and reports...");

        String defaultPasswordHash = passwordEncoder.encode("Admin@123");
        String ownerPasswordHash = passwordEncoder.encode("Owner@123");
        String studentPasswordHash = passwordEncoder.encode("Student@123");

        // 1. Admin User
        User admin = User.builder()
                .email("admin@parttimejob.com")
                .passwordHash(defaultPasswordHash)
                .fullName("System Administrator")
                .phone("+919876543210")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .suspended(false)
                .build();
        admin = userRepository.save(admin);

        // 2. Verified Owner: Sri Lakshmi Catering Services
        User ownerUser1 = User.builder()
                .email("owner.srilakshmi@catering.com")
                .passwordHash(ownerPasswordHash)
                .fullName("Ramesh Rao")
                .phone("+919845012345")
                .role(Role.ROLE_OWNER)
                .active(true)
                .suspended(false)
                .build();
        ownerUser1 = userRepository.save(ownerUser1);

        OwnerProfile ownerProfile1 = OwnerProfile.builder()
                .user(ownerUser1)
                .cateringName("Sri Lakshmi Catering Services")
                .businessAddress("124, 8th Main Road, Kengeri Satellite Town, Bangalore - 560060")
                .businessPhone("+919845012345")
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now().minusDays(30))
                .totalJobsPosted(4)
                .build();
        ownerProfile1 = ownerProfileRepository.save(ownerProfile1);

        // 3. Pending Owner: Royal Grand Events & Catering
        User ownerUser2 = User.builder()
                .email("owner.royal@catering.com")
                .passwordHash(ownerPasswordHash)
                .fullName("Suresh Kumar")
                .phone("+919845098765")
                .role(Role.ROLE_OWNER)
                .active(true)
                .suspended(false)
                .build();
        ownerUser2 = userRepository.save(ownerUser2);

        OwnerProfile ownerProfile2 = OwnerProfile.builder()
                .user(ownerUser2)
                .cateringName("Royal Grand Events & Catering")
                .businessAddress("55, 100ft Road, Indiranagar, Bangalore - 560038")
                .businessPhone("+919845098765")
                .verificationStatus(VerificationStatus.PENDING_VERIFICATION)
                .totalJobsPosted(1)
                .build();
        ownerProfile2 = ownerProfileRepository.save(ownerProfile2);

        // 4. Student 1: Pavan Kumar
        User studentUser1 = User.builder()
                .email("student.pavan@gmail.com")
                .passwordHash(studentPasswordHash)
                .fullName("Pavan Kumar")
                .phone("+919900112233")
                .role(Role.ROLE_STUDENT)
                .active(true)
                .suspended(false)
                .build();
        studentUser1 = userRepository.save(studentUser1);

        StudentProfile studentProfile1 = StudentProfile.builder()
                .user(studentUser1)
                .collegeName("Bangalore Institute of Technology")
                .preferredArea("Kengeri")
                .skills("Food Serving, Buffet Management, Table Setup, Guest Hospitality")
                .bio("Dedicated 3rd-year engineering student looking for weekend and evening part-time catering jobs. Punctual and hard-working.")
                .emergencyContact("+919900000001")
                .totalJobsCompleted(3)
                .rating(4.9)
                .build();
        studentProfile1 = studentProfileRepository.save(studentProfile1);

        // 5. Student 2: Ananya Sharma
        User studentUser2 = User.builder()
                .email("student.ananya@gmail.com")
                .passwordHash(studentPasswordHash)
                .fullName("Ananya Sharma")
                .phone("+919900445566")
                .role(Role.ROLE_STUDENT)
                .active(true)
                .suspended(false)
                .build();
        studentUser2 = userRepository.save(studentUser2);

        StudentProfile studentProfile2 = StudentProfile.builder()
                .user(studentUser2)
                .collegeName("R.V. College of Engineering")
                .preferredArea("Koramangala")
                .skills("Guest Hospitality, Welcome Drink Counter, Banquet Service, Table Cleaning")
                .bio("Experienced student helper with great communication skills and banqueting experience.")
                .emergencyContact("+919900000002")
                .totalJobsCompleted(2)
                .rating(4.8)
                .build();
        studentProfile2 = studentProfileRepository.save(studentProfile2);

        // 6. Catering Jobs
        // Job 1: Kengeri Evening Food Server
        CateringJob job1 = CateringJob.builder()
                .owner(ownerProfile1)
                .title("Evening Buffet Food Server")
                .description("Looking for energetic students to serve hot buffet dishes and maintain clean food counters for a high-profile wedding reception. Neat attire (black trousers & white shirt) provided/required. Dinner provided on-site.")
                .workType(WorkType.FOOD_SERVER)
                .workArea("Kengeri")
                .detailedLocation("Shubha Mangala Kalyana Mantapa, Near Kengeri Metro Station, Bangalore")
                .jobDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(23, 0))
                .paymentAmount(new BigDecimal("750.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(8)
                .workersSelected(1)
                .requiredSkills("Good communication, buffet counter discipline, neat uniform")
                .contactPhone("+919845012345")
                .contactEmail("owner.srilakshmi@catering.com")
                .status(JobStatus.OPEN)
                .build();
        job1 = jobRepository.save(job1);

        // Job 2: Koramangala Banquet Helpers
        CateringJob job2 = CateringJob.builder()
                .owner(ownerProfile1)
                .title("Banquet Hall Helpers & Table Setup")
                .description("Event helpers required for table arrangement, chair covers setup, water service, and dessert station restocking for an anniversary gala dinner.")
                .workType(WorkType.CATERING_HELPER)
                .workArea("Koramangala")
                .detailedLocation("The Grand Orchid Banquet, 5th Block, Koramangala, Bangalore")
                .jobDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(22, 0))
                .paymentAmount(new BigDecimal("700.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(5)
                .workersSelected(1)
                .requiredSkills("Punctuality, banquet setup, team coordination")
                .contactPhone("+919845012345")
                .contactEmail("owner.srilakshmi@catering.com")
                .status(JobStatus.OPEN)
                .build();
        job2 = jobRepository.save(job2);

        // Job 3: Jayanagar Kitchen Prep & Assistant
        CateringJob job3 = CateringJob.builder()
                .owner(ownerProfile1)
                .title("Morning Wedding Kitchen Assistant & Prep")
                .description("Assisting master chefs with vegetable chopping, ingredient staging, vessel transport, and breakfast buffet serving for traditional South Indian wedding.")
                .workType(WorkType.KITCHEN_HELPER)
                .workArea("Jayanagar")
                .detailedLocation("Sri Krishna Convention Centre, 4th Block Jayanagar, Bangalore")
                .jobDate(LocalDate.now().plusDays(4))
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(15, 0))
                .paymentAmount(new BigDecimal("900.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(4)
                .workersSelected(0)
                .requiredSkills("Kitchen assistance, vegetable prep, hygiene")
                .contactPhone("+919845012345")
                .contactEmail("owner.srilakshmi@catering.com")
                .status(JobStatus.OPEN)
                .build();
        job3 = jobRepository.save(job3);

        // Job 4: Indiranagar Event Staff (Pending owner)
        CateringJob job4 = CateringJob.builder()
                .owner(ownerProfile2)
                .title("VIP Lounge Event & Mocktail Server")
                .description("Serving welcome drinks and starters at an upscale product launch event in Indiranagar. Must be fluent in English/Kannada.")
                .workType(WorkType.EVENT_HELPER)
                .workArea("Indiranagar")
                .detailedLocation("Skyline Terrace Club, 100 Feet Road, Indiranagar, Bangalore")
                .jobDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(23, 30))
                .paymentAmount(new BigDecimal("850.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(6)
                .workersSelected(0)
                .requiredSkills("Hospitality, English speaking, tray handling")
                .contactPhone("+919845098765")
                .contactEmail("owner.royal@catering.com")
                .status(JobStatus.OPEN)
                .build();
        job4 = jobRepository.save(job4);

        // Job 5: Whitefield Corporate Lunch Server
        CateringJob job5 = CateringJob.builder()
                .owner(ownerProfile1)
                .title("Corporate Lunch Buffet Waiter")
                .description("Serving corporate employees during an annual tech conference. Clean, professional setup with buffet monitoring.")
                .workType(WorkType.WAITER)
                .workArea("Whitefield")
                .detailedLocation("ITPB Tech Park Amphitheatre, Whitefield, Bangalore")
                .jobDate(LocalDate.now().plusDays(6))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(16, 0))
                .paymentAmount(new BigDecimal("650.00"))
                .paymentType(PaymentType.ON_SPOT_PAYMENT)
                .onSpotPayment(true)
                .workersRequired(6)
                .workersSelected(0)
                .requiredSkills("Courteous behavior, lunch service, hygiene")
                .contactPhone("+919845012345")
                .contactEmail("owner.srilakshmi@catering.com")
                .status(JobStatus.OPEN)
                .build();
        job5 = jobRepository.save(job5);

        // 7. Seed Applications
        // Student 1 (Pavan) accepted on Job 1
        JobApplication app1 = JobApplication.builder()
                .job(job1)
                .student(studentProfile1)
                .status(ApplicationStatus.ACCEPTED)
                .appliedAt(LocalDateTime.now().minusDays(1))
                .respondedAt(LocalDateTime.now().minusHours(12))
                .attendanceStatus(AttendanceStatus.PRESENT)
                .workCompletionStatus("NOT_COMPLETED")
                .paymentStatus(PaymentStatus.PENDING)
                .paymentAmount(job1.getPaymentAmount())
                .notes("I have my own black formal trousers and shoes.")
                .build();
        app1 = applicationRepository.save(app1);

        // Student 2 (Ananya) accepted on Job 2
        JobApplication app2 = JobApplication.builder()
                .job(job2)
                .student(studentProfile2)
                .status(ApplicationStatus.ACCEPTED)
                .appliedAt(LocalDateTime.now().minusDays(2))
                .respondedAt(LocalDateTime.now().minusDays(1))
                .attendanceStatus(AttendanceStatus.PRESENT)
                .workCompletionStatus("NOT_COMPLETED")
                .paymentStatus(PaymentStatus.PENDING)
                .paymentAmount(job2.getPaymentAmount())
                .notes("I live 15 mins away from Koramangala 5th block.")
                .build();
        app2 = applicationRepository.save(app2);

        // Student 1 also applied to Job 4 (Royal Grand)
        JobApplication app3 = JobApplication.builder()
                .job(job4)
                .student(studentProfile1)
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDateTime.now().minusHours(3))
                .attendanceStatus(AttendanceStatus.NOT_MARKED)
                .workCompletionStatus("NOT_COMPLETED")
                .paymentStatus(PaymentStatus.PENDING)
                .paymentAmount(job4.getPaymentAmount())
                .notes("Available throughout the entire evening.")
                .build();
        app3 = applicationRepository.save(app3);

        // 8. Notifications
        Notification notif1 = Notification.builder()
                .recipient(studentUser1)
                .title("Application Accepted: " + job1.getTitle())
                .message("Sri Lakshmi Catering Services has accepted your application! Full address and contact number are unlocked.")
                .type(NotificationType.APPLICATION_ACCEPTED)
                .relatedEntityId(app1.getId())
                .read(false)
                .build();
        notificationRepository.save(notif1);

        Notification notif2 = Notification.builder()
                .recipient(ownerUser1)
                .title("New Application Received")
                .message("Pavan Kumar applied for '" + job1.getTitle() + "'.")
                .type(NotificationType.APPLICATION_RECEIVED)
                .relatedEntityId(app1.getId())
                .read(true)
                .build();
        notificationRepository.save(notif2);

        // 9. Sample Audit Log
        AuditLog audit1 = AuditLog.builder()
                .userId(admin.getId())
                .action("SYSTEM_INIT")
                .entityName("System")
                .entityId(1L)
                .details("System initialized with seed demo data")
                .ipAddress("127.0.0.1")
                .build();
        auditLogRepository.save(audit1);

        log.info("Database seeding successfully completed!");
    }
}
