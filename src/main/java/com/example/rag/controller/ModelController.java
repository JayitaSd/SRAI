package com.example.rag.controller;

import com.example.rag.model.SessionData;
import com.example.rag.service.SessionStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ModelController {

    private final ChatClient.Builder chatClientBuilder;
    private final SessionStore sessionStore;

    public ModelController(ChatClient.Builder builder, SessionStore sessionStore) {
        this.chatClientBuilder = builder;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        String message   = body.get("message");

        SessionData session = sessionStore.get(sessionId);
        if (session == null) {
            return ResponseEntity.badRequest()
                    .body("Session not found or expired. Please re-upload your document.");
        }

        VectorStore vectorStore = session.getVectorStore();

        ChatClient chatClient = chatClientBuilder
                .defaultSystem("""
                You are a helpful assistant that answers questions ONLY based on the provided context.
                - Do not use any external knowledge.
                - If the answer is not present in the context, reply with: "I don't have enough information in the provided documents to answer this."
                - Be concise, accurate, and direct.
                - Always stay faithful to the content in the documents.
                """)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(10)
                                        .similarityThreshold(0.55)
                                        .build())
                                .build()
                )
                .build();

        // HyDE
        String hydePrompt = """
            Generate a detailed, factual paragraph that would serve as a perfect answer
            to the following question, based on a document a user has uploaded.

            Question: %s

            Hypothetical Answer:
            """.formatted(message);

        String hypotheticalDoc = chatClient.prompt()
                .user(hydePrompt)
                .call()
                .content();

        SearchRequest hsr = SearchRequest.builder()
                .query(hypotheticalDoc)
                .topK(10)
                .similarityThreshold(0.55)
                .build();

        String hydeResponse = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("searchRequest", hsr))
                .call()
                .content();

        if (isValid(hydeResponse)) {
            session.addMessage("user", message);
            session.addMessage("assistant", hydeResponse);
            return ResponseEntity.ok(hydeResponse);
        }

        // Fallback
        SearchRequest normal = SearchRequest.builder()
                .query(message)
                .topK(8)
                .similarityThreshold(0.5)
                .build();

        String fallback = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("searchRequest", normal))
                .call()
                .content();

        session.addMessage("user", message);
        session.addMessage("assistant", fallback);
        return ResponseEntity.ok(fallback);
    }

    private boolean isValid(String response) {
        if (response == null || response.trim().isEmpty()) return false;
        String low = response.toLowerCase();
        return !low.contains("i don't have enough information") &&
                !low.contains("i don't know") &&
                !low.contains("no information") &&
                low.length() > 25;
    }
}