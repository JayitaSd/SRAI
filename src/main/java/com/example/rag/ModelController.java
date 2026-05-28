package com.example.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelController {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    public ModelController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
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
                                        .build()
                                )
                                .build()
                )
                .build();
    }
    @GetMapping("/rag/models")
    public String faq(@RequestParam String message) {
        //Generating hypothetical document
        String hydePrompt = """
                Generate a detailed, factual, and natural-sounding paragraph\s
                that would serve as a perfect answer to the following question.
                Use a style similar to an informative article about the NBA.
                Include specific names, years, and details where relevant.
            
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
                .advisors(a->a.param("searchRequest",hsr))
                .call()
                .content();
        if(isValid(hydeResponse)) return hydeResponse;

        //Fallback to regular retrieval if hyde response is not valid
        System.out.println("HyDE fallback triggered for : "+message);

        SearchRequest normal = SearchRequest.builder()
                .query(message)
                .topK(8)
                .similarityThreshold(0.5)
                .build();

        return chatClient.prompt()
                .user(message)
                .advisors(a->a.param("searchRequest",normal))
                .call()
                .content();
    }
    //detection of useless information
    private boolean isValid(String response) {
        if(response == null || response.trim().isEmpty()) return false;
        String low = response.toLowerCase();
        return !low.contains("i don't have enough information") &&
                !low.contains("i don't know") &&
                !low.contains("no information") &&
                low.length() > 25;
    }
}
