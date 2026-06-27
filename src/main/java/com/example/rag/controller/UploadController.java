package com.example.rag.controller;

import com.example.rag.model.SessionData;

import com.example.rag.service.DocExtractService;
import com.example.rag.service.GeminiExtractService;

import com.example.rag.service.SessionStore;
import com.example.rag.util.FileValidationUtil;
import com.example.rag.util.TextPreprocessor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Set<String> GEMINI_EXTRACT_TYPES = Set.of("png", "jpg", "jpeg");

    private final GeminiExtractService geminiExtractService;
    private final DocExtractService docExtractService;
    private final EmbeddingModel embeddingModel;
    private final SessionStore sessionStore;

    public UploadController(GeminiExtractService geminiExtractService,
                            DocExtractService docExtractService,
                            EmbeddingModel embeddingModel,
                            SessionStore sessionStore) {
        this.geminiExtractService = geminiExtractService;
        this.docExtractService = docExtractService;
        this.embeddingModel = embeddingModel;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {

        FileValidationUtil.ValidationResult validation = FileValidationUtil.validate(file);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of("error", validation.getError()));
        }

        try {
            String filename = file.getOriginalFilename();
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

            String rawText = GEMINI_EXTRACT_TYPES.contains(ext)
                    ? geminiExtractService.extractText(file)
                    : docExtractService.extractText(file);

            if (rawText == null || rawText.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No extractable text found in document."));
            }

            String cleaned = TextPreprocessor.cleanForEmbedding(rawText);

            Document document = new Document(cleaned, Map.of("filename", filename));

            TextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(750)
                    .withMinChunkSizeChars(200)
                    .withMinChunkLengthToEmbed(20)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = splitter.apply(List.of(document));

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            vectorStore.add(chunks);

            String sessionId = UUID.randomUUID().toString();
            sessionStore.put(sessionId, new SessionData(vectorStore));

            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "status", "ready",
                    "chunkCount", chunks.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process document: " + e.getMessage()));
        }
    }
}