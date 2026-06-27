package com.example.rag.config;

import com.example.rag.service.SessionStore;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@EnableScheduling
public class SessionCleanUpScheduler {
    private static final Logger log = Logger.getLogger(SessionCleanUpScheduler.class.getName());

    private final SessionStore sessionStore;

    public SessionCleanUpScheduler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Scheduled(fixedRate = 10 * 60 * 1000) // every 10 minutes
    public void cleanup() {
        int before = sessionStore.size();
        sessionStore.evictIdleSessions();
        int after = sessionStore.size();
        if (before != after) {
            log.info("Evicted " + (before - after) + " idle session(s). Active: " + after);
        }
    }
}