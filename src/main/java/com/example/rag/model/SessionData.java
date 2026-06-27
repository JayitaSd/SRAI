package com.example.rag.model;

import org.springframework.ai.vectorstore.VectorStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SessionData {
    private final VectorStore vectorStore;
    private final List<ChatMessage> history = new ArrayList<>();
    private volatile Instant lastAccessed = Instant.now();

    public SessionData(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public VectorStore getVectorStore() {
        touch();
        return vectorStore;
    }

    public List<ChatMessage> getHistory() {
        touch();
        return Collections.unmodifiableList(history);
    }

    public synchronized void addMessage(String role, String content) {
        history.add(new ChatMessage(role, content));
        touch();
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    private void touch() {
        lastAccessed = Instant.now();
    }
}