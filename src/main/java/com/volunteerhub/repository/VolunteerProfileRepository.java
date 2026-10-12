package com.volunteerhub.repository;

import com.volunteerhub.entity.VolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for VolunteerProfile entity.
 */
@Repository
public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long> {

    Optional<VolunteerProfile> findByUserId(Long userId);

    Optional<VolunteerProfile> findByUserEmail(String email);
}
