package com.query.ticket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String userId) {
        // 3 minute timeout — client reconnects automatically via useSSE hook
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("[SSE] Connection completed for user {}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("[SSE] Connection timed out for user {}", userId);
        });
        emitter.onError((e) -> {
            emitters.remove(userId);
            log.warn("[SSE] Connection error for user {}: {}", userId, e.getMessage());
        });

        // Replace old connection if user reconnects
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            try { old.complete(); } catch (Exception ignored) {}
        }

        return emitter;
    }

    /**
     * Send event to a specific user.
     * Uses @Async so it never blocks the calling thread (ticket save, etc.)
     * and avoids Spring Security context propagation issues.
     */
    @Async("emailTaskExecutor")
    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("[SSE] Send failed for user {}, removing emitter", userId);
        } catch (Exception e) {
            emitters.remove(userId);
            log.warn("[SSE] Unexpected error for user {}: {}", userId, e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    public void sendToAll(String eventName, Object data) {
        emitters.keySet().forEach(userId -> sendToUser(userId, eventName, data));
    }

    public boolean isConnected(String userId) {
        return emitters.containsKey(userId);
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}