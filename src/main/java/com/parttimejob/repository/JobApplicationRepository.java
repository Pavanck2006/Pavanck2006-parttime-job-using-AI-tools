package com.parttimejob.repository;

import com.parttimejob.entity.CateringJob;
import com.parttimejob.entity.JobApplication;
import com.parttimejob.entity.StudentProfile;
import com.parttimejob.enums.ApplicationStatus;
import com.parttimejob.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByJobIdAndStudentId(Long jobId, Long studentId);

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    List<JobApplication> findByStudentIdOrderByAppliedAtDesc(Long studentId);

    List<JobApplication> findByStudentIdAndStatusOrderByAppliedAtDesc(Long studentId, ApplicationStatus status);

    List<JobApplication> findByJobIdOrderByAppliedAtAsc(Long jobId);

    List<JobApplication> findByJobIdAndStatus(Long jobId, ApplicationStatus status);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    List<JobApplication> findByJobOwnerIdOrderByAppliedAtDesc(Long ownerId);
}
