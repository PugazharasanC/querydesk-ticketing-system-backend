package com.query.ticket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // One emitter per user — new connection replaces old one
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String userId) {
        // 5 minute timeout — client reconnects automatically
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        emitters.put(userId, emitter);
        log.info("SSE connection opened for user: {}", userId);
        return emitter;
    }

    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE send failed for user {}, connection removed", userId);
        }
    }

    public void sendToAll(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> sendToUser(userId, eventName, data));
    }

    public boolean isConnected(String userId) {
        return emitters.containsKey(userId);
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}