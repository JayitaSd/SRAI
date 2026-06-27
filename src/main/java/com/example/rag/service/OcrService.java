package com.example.rag.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class OcrService {
    private final Tesseract tesseract;
    public OcrService(@Value("${tess4j.datapath}") String dataPath, @Value("${tess4j.language}") String language) {
        tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
    }
    public String extractText(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Could not read image from file: " + file.getOriginalFilename());
        }
        return tesseract.doOCR(image);
    }
}
