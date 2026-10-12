package com.volunteerhub.service;

import com.volunteerhub.dto.NgoProfileDto;
import com.volunteerhub.dto.NgoProfileUpdateRequest;
import com.volunteerhub.entity.NgoProfile;
import com.volunteerhub.entity.User;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing NGO organization profile views and edits.
 */
@Service
public class NgoProfileService {

    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;

    public NgoProfileService(UserRepository userRepository, NgoProfileRepository ngoProfileRepository) {
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
    }

    @Transactional(readOnly = true)
    public NgoProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        NgoProfile profile = ngoProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user: " + email));

        return toDto(profile);
    }

    @Transactional
    public NgoProfileDto updateProfile(String email, NgoProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        NgoProfile profile = ngoProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user: " + email));

        profile.setName(request.getName().trim());
        profile.setCause(request.getCause().trim());
        profile.setDescription(request.getDescription());
        profile.setContactPerson(request.getContactPerson().trim());
        profile.setContactPhone(request.getContactPhone().trim());
        profile.setAddress(request.getAddress().trim());

        profile = ngoProfileRepository.save(profile);
        return toDto(profile);
    }

    private NgoProfileDto toDto(NgoProfile profile) {
        return new NgoProfileDto(
            profile.getId(),
            profile.getUser().getId(),
            profile.getUser().getEmail(),
            profile.getName(),
            profile.getCause(),
            profile.getDescription(),
            profile.getRegistrationNumber(),
            profile.getContactPerson(),
            profile.getContactPhone(),
            profile.getAddress(),
            profile.getVerificationStatus(),
            profile.getRejectionReason(),
            profile.getCreatedAt()
        );
    }
}
