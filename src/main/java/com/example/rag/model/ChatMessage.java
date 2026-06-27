package com.example.rag.model;

import java.time.Instant;

public class ChatMessage {
    private final String role; // "user" or "assistant"
    private final String content;
    private final Instant timestamp;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getTimestamp() { return timestamp; }
}