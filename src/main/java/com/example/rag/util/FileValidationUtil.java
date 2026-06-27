package com.example.rag.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class FileValidationUtil {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "txt", "doc", "docx", "png", "jpg", "jpeg"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg"
    );

    public static ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.fail("No file provided.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            return ValidationResult.fail("File exceeds 10MB limit.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return ValidationResult.fail("File has no extension.");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return ValidationResult.fail("Unsupported file type: ." + ext);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ValidationResult.fail("File content type does not match an allowed type.");
        }

        return ValidationResult.ok();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String error) { return new ValidationResult(false, error); }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
    }
}