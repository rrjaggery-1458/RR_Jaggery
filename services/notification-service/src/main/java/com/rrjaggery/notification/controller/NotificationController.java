package com.rrjaggery.notification.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.notification.dto.CreateNotificationRequest;
import com.rrjaggery.notification.dto.NotificationResponse;
import com.rrjaggery.notification.dto.UnreadCountResponse;
import com.rrjaggery.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private List<String> extractRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of("ADMIN", "MANAGER");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        List<String> roles = extractRoles(authentication);
        List<NotificationResponse> list = notificationService.getNotifications(null, roles, unreadOnly);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        List<String> roles = extractRoles(authentication);
        UnreadCountResponse resp = notificationService.getUnreadCount(null, roles);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse resp = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(resp, "Notification created successfully"));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        NotificationResponse resp = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(resp, "Notification marked as read"));
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(Authentication authentication) {
        List<String> roles = extractRoles(authentication);
        int count = notificationService.markAllAsRead(null, roles);
        return ResponseEntity.ok(ApiResponse.ok(count, "Marked " + count + " notifications as read"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted successfully"));
    }

    @PostMapping("/sync-operational-alerts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<String>> syncOperationalAlerts(
            @RequestBody(required = false) List<CreateNotificationRequest> alerts) {
        int added = 0;
        if (alerts != null) {
            for (CreateNotificationRequest a : alerts) {
                boolean created = notificationService.createUniqueAlertIfNotExists(
                        a.getType(),
                        a.getSeverity(),
                        a.getTitle(),
                        a.getMessage(),
                        a.getReferenceId(),
                        a.getReferenceType(),
                        a.getRole()
                );
                if (created) added++;
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Synced operational alerts. Newly created: " + added));
    }
}
