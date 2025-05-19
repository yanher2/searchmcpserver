package com.searchserver.service;

import com.searchserver.model.LaptopInfo;
import com.searchserver.repository.LaptopInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaptopSearchService {

    private final LaptopInfoRepository laptopInfoRepository;
    private final EmbeddingService embeddingService;
    private final JdCrawlerService crawlerService;

    public List<LaptopInfo> searchByKeyword(String keyword) {
        return laptopInfoRepository.searchByKeyword(keyword);
    }

    public List<LaptopInfo> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return laptopInfoRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<LaptopInfo> findSimilarLaptops(String description, int limit) {
        double[] embedding = embeddingService.generateEmbedding(description);
        return laptopInfoRepository.findSimilarLaptops(embedding, limit);
    }

    public List<LaptopInfo> findSimilarLaptops(Long laptopId, int limit) {
        LaptopInfo laptop = laptopInfoRepository.findById(laptopId)
                .orElseThrow(() -> new IllegalArgumentException("Laptop not found with id: " + laptopId));

        return laptopInfoRepository.findSimilarLaptops(laptop.getEmbedding(), limit).stream()
                .filter(l -> !l.getId().equals(laptopId))
                .collect(Collectors.toList());
    }

    public List<LaptopInfo> findByBrand(String brand) {
        return laptopInfoRepository.findByBrand(brand);
    }

    public void refreshLaptopData() {
        try {
            List<LaptopInfo> newLaptops = crawlerService.crawlJdSecondHandLaptops();
            log.info("Successfully refreshed laptop data. Found {} new laptops", newLaptops.size());
        } catch (Exception e) {
            log.error("Error refreshing laptop data", e);
            throw new RuntimeException("Failed to refresh laptop data", e);
        }
    }

    public LaptopInfo findByProductId(String productId) {
        return laptopInfoRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Laptop not found with product id: " + productId));
    }
}
