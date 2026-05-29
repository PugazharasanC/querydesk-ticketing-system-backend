package com.query.ticket.controller;

import com.query.ticket.security.CurrentUser;
import com.query.ticket.security.UserPrincipal;
import com.query.ticket.service.NotificationService;
import com.query.ticket.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "Server-Sent Events for real-time updates")
@SecurityRequirement(name = "bearerAuth")
public class SseController {

    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subscribe to real-time notifications")
    public SseEmitter subscribe(@CurrentUser UserPrincipal currentUser) {
        SseEmitter emitter = sseEmitterService.createEmitter(currentUser.getId());

        // Send unread count immediately on connect
        long unread = notificationService.getUnreadCount(currentUser.getId());
        sseEmitterService.sendToUser(currentUser.getId(), "unread_count", String.valueOf(unread));

        return emitter;
    }
}