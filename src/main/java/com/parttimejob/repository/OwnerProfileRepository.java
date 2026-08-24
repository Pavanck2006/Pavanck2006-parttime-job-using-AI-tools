package com.parttimejob.repository;

import com.parttimejob.entity.OwnerProfile;
import com.parttimejob.entity.User;
import com.parttimejob.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    Optional<OwnerProfile> findByUser(User user);
    Optional<OwnerProfile> findByUserId(Long userId);
    Optional<OwnerProfile> findByUserEmail(String email);
    List<OwnerProfile> findByVerificationStatus(VerificationStatus status);
    long countByVerificationStatus(VerificationStatus status);
}
