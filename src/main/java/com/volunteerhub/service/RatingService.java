package com.volunteerhub.service;

import com.volunteerhub.dto.RatingCreateRequest;
import com.volunteerhub.dto.VolunteerRatingDto;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.AttendanceRepository;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.repository.VolunteerRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing volunteer performance ratings and evaluations.
 */
@Service
public class RatingService {

    private final VolunteerRatingRepository volunteerRatingRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final NotificationService notificationService;

    public RatingService(VolunteerRatingRepository volunteerRatingRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository,
                         NgoProfileRepository ngoProfileRepository,
                         NotificationService notificationService) {
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.notificationService = notificationService;
    }

    /**
     * Submits a performance rating for a volunteer's shift on an event.
     * Restricted to the owning NGO of the initiative, and only applicable for PRESENT volunteers.
     */
    @Transactional
    public VolunteerRatingDto createRating(Long attendanceId, String ngoEmail, RatingCreateRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + attendanceId));

        User ngoUser = userRepository.findByEmail(ngoEmail)
                .orElseThrow(() -> new ResourceNotFoundException("NGO user not found: " + ngoEmail));

        Event event = attendance.getApplication().getEvent();

        // 1. Ownership check: Only hosting NGO can rate
        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to evaluate attendance for another organization's event");
        }

        // 2. Status check: Only PRESENT volunteers can receive a rating
        if (attendance.getAttendanceStatus() != AttendanceStatus.PRESENT) {
            throw new BadRequestException("Cannot submit performance evaluation for an absent volunteer");
        }

        // 3. Duplicate check: Exactly one rating per attendance record
        if (volunteerRatingRepository.existsByAttendanceId(attendanceId)) {
            throw new ConflictException("A performance rating has already been submitted for this attendance record");
        }

        // 4. Create and persist VolunteerRating
        VolunteerRating rating = new VolunteerRating(
                attendance,
                request.getPunctuality(),
                request.getTeamwork(),
                request.getEffort(),
                request.getComment() != null ? request.getComment().trim() : null,
                ngoUser
        );
        VolunteerRating saved = volunteerRatingRepository.save(rating);

        // 5. Notify the volunteer
        User volunteer = attendance.getApplication().getVolunteer();
        String ngoName = ngoProfileRepository.findByUserId(ngoUser.getId())
                .map(p -> p.getName())
                .orElse("Event Organizer");

        double avgScore = roundToTwoDecimals(saved.getAverageScore());
        notificationService.createNotification(
                volunteer,
                "RATING_RECEIVED",
                "Performance Rating Received",
                String.format("%s rated your contribution on '%s' with an average score of %.2f/5.",
                        ngoName, event.getTitle(), avgScore),
                "ATTENDANCE",
                attendance.getId()
        );

        return toDto(saved, ngoName, event);
    }

    /**
     * Retrieves all ratings awarded to the authenticated volunteer.
     */
    @Transactional(readOnly = true)
    public List<VolunteerRatingDto> getVolunteerRatings(String volunteerEmail) {
        User volunteer = userRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer user not found: " + volunteerEmail));

        List<VolunteerRating> ratings = volunteerRatingRepository.findByVolunteerId(volunteer.getId());

        return ratings.stream().map(r -> {
            Event event = r.getAttendance().getApplication().getEvent();
            String ngoName = ngoProfileRepository.findByUserId(event.getNgo().getId())
                    .map(p -> p.getName())
                    .orElse("Event Organizer");
            return toDto(r, ngoName, event);
        }).collect(Collectors.toList());
    }

    private VolunteerRatingDto toDto(VolunteerRating rating, String ngoName, Event event) {
        return new VolunteerRatingDto(
                rating.getId(),
                rating.getAttendance().getId(),
                event.getId(),
                event.getTitle(),
                ngoName,
                rating.getPunctuality(),
                rating.getTeamwork(),
                rating.getEffort(),
                roundToTwoDecimals(rating.getAverageScore()),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
