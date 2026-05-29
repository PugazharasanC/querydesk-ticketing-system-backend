package com.query.ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.query.ticket.dto.response.NotificationResponse;
import com.query.ticket.enums.NotificationType;
import com.query.ticket.model.Notification;
import com.query.ticket.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    // ── Create & push ─────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void notify(String userId, NotificationType type,
                       String title, String message, String ticketId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .ticketId(ticketId)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);

        // Push via SSE if user is connected
        sseEmitterService.sendToUser(userId, "notification", toJson(response));
        log.info("Notification sent to user {}: {}", userId, title);
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public void notifyTicketCreated(String userId, String ticketId, String title) {
        notify(userId, NotificationType.TICKET_CREATED,
                "Ticket Created",
                "Your ticket \"" + title + "\" has been submitted successfully.",
                ticketId);
    }

    public void notifyTicketAssigned(String userId, String ticketId, String title, String assignedBy) {
        notify(userId, NotificationType.TICKET_ASSIGNED,
                "Ticket Assigned",
                "Ticket \"" + title + "\" has been assigned to you by " + assignedBy + ".",
                ticketId);
    }

    public void notifyStatusChanged(String userId, String ticketId, String title, String newStatus) {
        notify(userId, NotificationType.TICKET_STATUS_CHANGED,
                "Ticket Updated",
                "Ticket \"" + title + "\" status changed to " + newStatus.replace("_", " ") + ".",
                ticketId);
    }

    public void notifyEscalated(String userId, String ticketId, String title) {
        notify(userId, NotificationType.TICKET_ESCALATED,
                "Ticket Escalated",
                "Your ticket \"" + title + "\" has been escalated and will be handled urgently.",
                ticketId);
    }

    public void notifyCommentAdded(String userId, String ticketId, String title, String authorName) {
        notify(userId, NotificationType.TICKET_COMMENT_ADDED,
                "New Comment",
                authorName + " commented on ticket \"" + title + "\".",
                ticketId);
    }

    // ── Read / mark ───────────────────────────────────────────────────────────

    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public List<NotificationResponse> getUnread(String userId) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
                // Push updated unread count via SSE
                sseEmitterService.sendToUser(userId, "unread_count",
                        String.valueOf(getUnreadCount(userId)));
            }
        });
    }

    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        sseEmitterService.sendToUser(userId, "unread_count", "0");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .ticketId(n.getTicketId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}