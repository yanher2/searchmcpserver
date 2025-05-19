package com.searchserver.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class EmbeddingService {

    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public double[] generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return new double[384]; // Return zero vector for empty text
            }

            Embedding embedding = embeddingModel.embed(text).content();
            return embedding.vector();
        } catch (Exception e) {
            log.error("Error generating embedding for text", e);
            return new double[384]; // Return zero vector on error
        }
    }

    public double calculateCosineSimilarity(double[] vector1, double[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }
}
