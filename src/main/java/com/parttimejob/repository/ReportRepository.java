package com.parttimejob.repository;

import com.parttimejob.entity.Report;
import com.parttimejob.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReportStatus status);
}
