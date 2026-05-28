package com.example.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Configuration
public class RagConfig {
    private static final Logger log =  Logger.getLogger(RagConfig.class.getName());
    @Value("vectorstore.json")
    private String vectorStoreName;

    @Value("classpath:/data/document.txt")
    private Resource documentResource;

    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) throws IOException {
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var vectorStoreFile = getVectorStoreFile();
        if (vectorStoreFile.exists() && vectorStoreFile.length()>0) {
            log.info("Vector Store File Exists,");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            log.info("Vector Store File Does Not Exist, loading documents");
            TextReader textReader = new TextReader(documentResource);
            textReader.getCustomMetadata().put("filename", "document.txt");
            List<Document> documents = textReader.get();
            List<Document> cleanedDocs = documents.stream()
                    .map(doc -> new Document(
                            TextPreprocessor.cleanForEmbedding(doc.getText()),
                            doc.getMetadata()
                    ))
                    .toList();
            TextSplitter textSplitter = TokenTextSplitter.builder()
                    .withChunkSize(750)
                    .withMinChunkSizeChars(200)
                    .withMinChunkLengthToEmbed(20)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();
            List<Document> splitDocuments = textSplitter.apply(cleanedDocs);
            simpleVectorStore.add(splitDocuments);
            simpleVectorStore.save(vectorStoreFile);
            log.info("Successfully added document chunks to vector store");
        }
        return simpleVectorStore;
    }

    private File getVectorStoreFile() {
        Path path = Paths.get("src", "main", "resources", "data");
        String absolutePath = path.toFile().getAbsolutePath() + "/" + vectorStoreName;
        return new File(absolutePath);
    }
}
