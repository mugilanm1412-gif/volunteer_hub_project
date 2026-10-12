package com.volunteerhub.service;

import com.volunteerhub.dto.NotificationDto;
import com.volunteerhub.dto.PageResponseDto;
import com.volunteerhub.entity.Notification;
import com.volunteerhub.entity.User;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.NotificationRepository;
import com.volunteerhub.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing in-app notifications and unread badges.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(User user, String type, String title, String message,
                                           String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification(user, type, title, message, relatedEntityType, relatedEntityId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<NotificationDto> getNotifications(String email, Pageable pageable) {
        User user = findUserByEmail(email);
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        Page<NotificationDto> dtoPage = page.map(this::toDto);
        return PageResponseDto.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = findUserByEmail(email);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = findUserByEmail(email);
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access notifications belonging to another user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = findUserByEmail(email);
        List<Notification> unreadList = notificationRepository.findByUserIdAndReadFalse(user.getId());
        for (Notification notification : unreadList) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadList);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.isRead(),
            notification.getRelatedEntityType(),
            notification.getRelatedEntityId(),
            notification.getCreatedAt()
        );
    }
}
