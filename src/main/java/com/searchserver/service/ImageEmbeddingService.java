package com.searchserver.service;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

@Service
public class ImageEmbeddingService {

    private static final int IMAGE_EMBEDDING_SIZE = 512;

    public double[] generateEmbedding(String imageUrl) throws IOException, ModelException, TranslateException {
        try (ZooModel<Image, float[]> model = loadModel()) {
            try (Predictor<Image, float[]> predictor = model.newPredictor()) {
                Image image = loadImage(imageUrl);
                float[] embedding = predictor.predict(image);
                return toDoubleArray(embedding);
            }
        }
    }

    private ZooModel<Image, float[]> loadModel() throws ModelException, IOException {
        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .optFilter("backbone", "resnet50")
                .build();
        return ModelZoo.loadModel(criteria);
    }

    private Image loadImage(String imageUrl) throws IOException {
        if (imageUrl.startsWith("http")) {
            return ImageFactory.getInstance().fromUrl(new URL(imageUrl));
        } else {
            return ImageFactory.getInstance().fromFile(Path.of(imageUrl));
        }
    }

    private double[] toDoubleArray(float[] floatArray) {
        double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }
        return doubleArray;
    }
}
