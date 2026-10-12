package com.volunteerhub.repository;

import com.volunteerhub.entity.NgoProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for NgoProfile entity.
 */
@Repository
public interface NgoProfileRepository extends JpaRepository<NgoProfile, Long> {

    Optional<NgoProfile> findByUserId(Long userId);

    Optional<NgoProfile> findByUserEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    java.util.List<NgoProfile> findByVerificationStatus(com.volunteerhub.entity.NgoVerificationStatus verificationStatus);

    long countByVerificationStatus(com.volunteerhub.entity.NgoVerificationStatus verificationStatus);
}
