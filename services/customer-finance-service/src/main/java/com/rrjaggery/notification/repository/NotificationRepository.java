package com.rrjaggery.notification.repository;

import com.rrjaggery.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE (n.userId = :userId OR (n.role IN :roles OR n.role IS NULL OR n.role = 'ALL')) ORDER BY n.createdAt DESC")
    List<Notification> findForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles);

    @Query("SELECT n FROM Notification n WHERE (n.userId = :userId OR (n.role IN :roles OR n.role IS NULL OR n.role = 'ALL')) AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.userId = :userId OR (n.role IN :roles OR n.role IS NULL OR n.role = 'ALL')) AND n.isRead = false")
    long countUnreadForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE (n.userId = :userId OR (n.role IN :roles OR n.role IS NULL OR n.role = 'ALL')) AND n.isRead = false")
    int markAllAsReadForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles, @Param("readAt") OffsetDateTime readAt);

    boolean existsByTypeAndReferenceIdAndIsReadFalse(String type, String referenceId);
}
