package com.example.rag.util;

public class TextPreprocessor {
    public static String cleanForEmbedding(String text) {
        if (text == null) return "";

        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")      // Remove special characters only
                .replaceAll("\\s+", " ")              // Normalize whitespace
                .trim();
    }
}