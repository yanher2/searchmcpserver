package com.searchserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaptopInfo {

    private Long id;
    private String title;
    private String description;
    private String productId;
    private String imageUrl;
    private String productUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String brand;
    private String model;
    private String processorInfo;
    private String memoryInfo;
    private String storageInfo;
    private String displayInfo;
    private String conditionGrade;
    private String sellerName;
    private Double sellerRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double[] embedding;
    private double[] imageEmbedding;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProcessorInfo() {
        return processorInfo;
    }

    public void setProcessorInfo(String processorInfo) {
        this.processorInfo = processorInfo;
    }

    public String getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(String memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public String getStorageInfo() {
        return storageInfo;
    }

    public void setStorageInfo(String storageInfo) {
        this.storageInfo = storageInfo;
    }

    public String getDisplayInfo() {
        return displayInfo;
    }

    public void setDisplayInfo(String displayInfo) {
        this.displayInfo = displayInfo;
    }

    public String getConditionGrade() {
        return conditionGrade;
    }

    public void setConditionGrade(String conditionGrade) {
        this.conditionGrade = conditionGrade;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Double getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(Double sellerRating) {
        this.sellerRating = sellerRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }
    
    public double[] getImageEmbedding() {
        return imageEmbedding;
    }

    public void setImageEmbedding(double[] imageEmbedding) {
        this.imageEmbedding = imageEmbedding;
    }
}
