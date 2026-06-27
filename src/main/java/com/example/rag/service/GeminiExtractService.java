package com.example.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class GeminiExtractService {

    private final ChatClient chatClient;

    private static final String EXTRACTION_PROMPT = """
            Extract ALL text content from this document exactly as it appears.
            - Preserve the original wording and order.
            - Do not summarize, interpret, or omit anything.
            - Do not add commentary before or after the extracted text.
            - If the document contains tables, represent them as plain text rows.
            """;

    public GeminiExtractService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String extractText(MultipartFile file) throws IOException {
        MimeType mimeType = MimeType.valueOf(file.getContentType());
        Media media = Media.builder()
                .mimeType(mimeType)
                .data(file.getResource())
                .build();

        UserMessage message = UserMessage.builder()
                .text(EXTRACTION_PROMPT)
                .media(media)
                .build();

        return chatClient.prompt()
                .messages(message)
                .call()
                .content();
    }
}