package com.parttimejob.repository;

import com.parttimejob.entity.PaymentRecord;
import com.parttimejob.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByApplicationId(Long applicationId);

    List<PaymentRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<PaymentRecord> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<PaymentRecord> findByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRecord p WHERE p.paymentStatus IN ('PAID', 'CONFIRMED')")
    BigDecimal sumTotalPaidAmount();
}
