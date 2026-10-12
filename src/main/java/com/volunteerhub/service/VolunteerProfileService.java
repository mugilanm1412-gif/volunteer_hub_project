package com.volunteerhub.service;

import com.volunteerhub.dto.VolunteerProfileDto;
import com.volunteerhub.dto.VolunteerProfileUpdateRequest;
import com.volunteerhub.entity.User;
import com.volunteerhub.entity.VolunteerProfile;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.repository.VolunteerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing volunteer profile lookups and updates.
 */
@Service
public class VolunteerProfileService {

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;

    public VolunteerProfileService(UserRepository userRepository, VolunteerProfileRepository volunteerProfileRepository) {
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
    }

    @Transactional(readOnly = true)
    public VolunteerProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        VolunteerProfile profile = volunteerProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user: " + email));

        return toDto(profile);
    }

    @Transactional
    public VolunteerProfileDto updateProfile(String email, VolunteerProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        VolunteerProfile profile = volunteerProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user: " + email));

        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone());
        profile.setCity(request.getCity());
        profile.setSkills(request.getSkills());
        profile.setInterests(request.getInterests());
        profile.setBio(request.getBio());
        profile.setShowOnLeaderboard(request.isShowOnLeaderboard());

        profile = volunteerProfileRepository.save(profile);
        return toDto(profile);
    }

    private VolunteerProfileDto toDto(VolunteerProfile profile) {
        return new VolunteerProfileDto(
            profile.getId(),
            profile.getUser().getId(),
            profile.getUser().getEmail(),
            profile.getFullName(),
            profile.getPhone(),
            profile.getCity(),
            profile.getSkills(),
            profile.getInterests(),
            profile.getBio(),
            profile.isShowOnLeaderboard(),
            profile.getCreatedAt()
        );
    }
}
