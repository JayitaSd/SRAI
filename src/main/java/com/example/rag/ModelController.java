package com.example.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelController {
    private final ChatClient chatClient;

    public ModelController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore).
                                build()
                )
                .build();
    }
    @GetMapping("/rag/models")
    public String faq(@RequestParam String message) {
        var instructions = """
                You are a helpful assistant that answers questions ONLY based on the provided context.
                - Do not use any external knowledge.
                - If the answer is not present in the context, reply with: "I don't have enough information in the provided documents to answer this."
                - Be concise, accurate, and direct.
                - Always stay faithful to the content in the documents.
                """;
        return chatClient.prompt()
                .user(message)
                .system(instructions)
                .call()
                .content();
    }
}
