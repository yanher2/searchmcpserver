package com.searchserver.model;

import io.hypersistence.utils.hibernate.type.array.DoubleArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "laptop_info")
@TypeDef(name = "double-array", typeClass = DoubleArrayType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaptopInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "product_id", unique = true)
    private String productId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "product_url")
    private String productUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    private String brand;

    private String model;

    @Column(name = "processor_info")
    private String processorInfo;

    @Column(name = "memory_info")
    private String memoryInfo;

    @Column(name = "storage_info")
    private String storageInfo;

    @Column(name = "display_info")
    private String displayInfo;

    @Column(name = "condition_grade")
    private String conditionGrade;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "seller_rating")
    private Double sellerRating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Type(type = "double-array")
    @Column(name = "embedding", columnDefinition = "float8[]")
    private double[] embedding;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
