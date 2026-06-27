package com.example.rag.service;

import com.example.rag.model.SessionData;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    // 30 min idle timeout — tune as needed
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public void put(String sessionId, SessionData data) {
        sessions.put(sessionId, data);
    }

    public SessionData get(String sessionId) {
        return sessions.get(sessionId);
    }

    public boolean exists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    /** Called by the scheduled cleanup task. */
    public void evictIdleSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(e -> e.getValue().getLastAccessed().isBefore(cutoff));
    }

    public int size() {
        return sessions.size();
    }
}