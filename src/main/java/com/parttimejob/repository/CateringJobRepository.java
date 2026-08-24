package com.parttimejob.repository;

import com.parttimejob.entity.CateringJob;
import com.parttimejob.enums.JobStatus;
import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.WorkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CateringJobRepository extends JpaRepository<CateringJob, Long> {

    List<CateringJob> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<CateringJob> findByStatusOrderByJobDateAsc(JobStatus status);

    List<CateringJob> findByStatusInOrderByJobDateAsc(List<JobStatus> statuses);

    long countByStatus(JobStatus status);

    @Query("SELECT j FROM CateringJob j WHERE " +
            "(:status IS NULL OR j.status = :status) AND " +
            "(:area IS NULL OR LOWER(j.workArea) LIKE LOWER(CONCAT('%', :area, '%'))) AND " +
            "(:location IS NULL OR LOWER(j.detailedLocation) LIKE LOWER(CONCAT('%', :location, '%')) OR LOWER(j.workArea) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:workType IS NULL OR j.workType = :workType) AND " +
            "(:jobDate IS NULL OR j.jobDate = :jobDate) AND " +
            "(:minPayment IS NULL OR j.paymentAmount >= :minPayment) AND " +
            "(:maxPayment IS NULL OR j.paymentAmount <= :maxPayment) AND " +
            "(:paymentType IS NULL OR j.paymentType = :paymentType) AND " +
            "(:startTime IS NULL OR j.startTime >= :startTime) AND " +
            "(:endTime IS NULL OR j.endTime <= :endTime) " +
            "ORDER BY j.jobDate ASC, j.startTime ASC")
    List<CateringJob> searchJobs(
            @Param("status") JobStatus status,
            @Param("area") String area,
            @Param("location") String location,
            @Param("workType") WorkType workType,
            @Param("jobDate") LocalDate jobDate,
            @Param("minPayment") BigDecimal minPayment,
            @Param("maxPayment") BigDecimal maxPayment,
            @Param("paymentType") PaymentType paymentType,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("SELECT j FROM CateringJob j WHERE j.status = 'OPEN' AND " +
            "(LOWER(j.workArea) LIKE LOWER(CONCAT('%', :preferredArea, '%')) OR :preferredArea IS NULL) " +
            "ORDER BY j.paymentAmount DESC, j.jobDate ASC")
    List<CateringJob> findRecommendedJobs(@Param("preferredArea") String preferredArea);
}
