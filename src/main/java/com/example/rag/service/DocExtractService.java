package com.example.rag.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocExtractService {

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        if (ext.equals("pdf")) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        }

        if (ext.equals("txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        if (ext.equals("docx")) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }

        throw new IllegalArgumentException("Unsupported extension for local extraction: " + ext);
        // Note: legacy .doc (binary) needs HWPFDocument/WordExtractor from poi-scratchpad
        // if you need to support it — flag if so, it's a separate POI dependency.
    }
}