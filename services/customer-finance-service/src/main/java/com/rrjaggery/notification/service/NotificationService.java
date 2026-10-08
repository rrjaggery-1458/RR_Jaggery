package com.rrjaggery.notification.service;

import com.rrjaggery.notification.dto.CreateNotificationRequest;
import com.rrjaggery.notification.dto.NotificationResponse;
import com.rrjaggery.notification.dto.UnreadCountResponse;
import com.rrjaggery.notification.model.Notification;
import com.rrjaggery.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId, List<String> roles, boolean unreadOnly) {
        List<String> cleanRoles = roles != null && !roles.isEmpty() ? roles : List.of("ADMIN", "MANAGER");
        List<Notification> list;
        if (unreadOnly) {
            list = notificationRepository.findUnreadForUserAndRoles(userId, cleanRoles);
        } else {
            list = notificationRepository.findForUserAndRoles(userId, cleanRoles);
        }
        return list.stream().map(NotificationResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId, List<String> roles) {
        List<String> cleanRoles = roles != null && !roles.isEmpty() ? roles : List.of("ADMIN", "MANAGER");
        long count = notificationRepository.countUnreadForUserAndRoles(userId, cleanRoles);
        return new UnreadCountResponse(count);
    }

    public NotificationResponse createNotification(CreateNotificationRequest req) {
        Notification n = new Notification(
                req.getUserId(),
                req.getRole() != null ? req.getRole().toUpperCase() : null,
                req.getType().toUpperCase(),
                req.getSeverity() != null ? req.getSeverity().toUpperCase() : "INFO",
                req.getTitle(),
                req.getMessage(),
                req.getReferenceId(),
                req.getReferenceType()
        );
        n = notificationRepository.save(n);
        log.info("Created notification id={} type={} title={}", n.getId(), n.getType(), n.getTitle());
        return NotificationResponse.fromEntity(n);
    }

    public NotificationResponse markAsRead(UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
        n.setRead(true);
        n.setReadAt(OffsetDateTime.now());
        n = notificationRepository.save(n);
        return NotificationResponse.fromEntity(n);
    }

    public int markAllAsRead(UUID userId, List<String> roles) {
        List<String> cleanRoles = roles != null && !roles.isEmpty() ? roles : List.of("ADMIN", "MANAGER");
        return notificationRepository.markAllAsReadForUserAndRoles(userId, cleanRoles, OffsetDateTime.now());
    }

    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }

    public boolean createUniqueAlertIfNotExists(String type, String severity, String title, String message, String referenceId, String referenceType, String role) {
        if (referenceId != null && notificationRepository.existsByTypeAndReferenceIdAndIsReadFalse(type, referenceId)) {
            return false;
        }
        CreateNotificationRequest req = new CreateNotificationRequest(null, role, type, severity, title, message, referenceId, referenceType);
        createNotification(req);
        return true;
    }
}
