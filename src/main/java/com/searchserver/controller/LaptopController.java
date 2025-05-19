package com.searchserver.controller;

import com.searchserver.model.LaptopInfo;
import com.searchserver.service.LaptopSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/laptops")
@RequiredArgsConstructor
public class LaptopController {
    @Resource
    private LaptopSearchService laptopSearchService;
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(LaptopController.class);
    @GetMapping("/search")
    public ResponseEntity<List<LaptopInfo>> searchLaptops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        try {
            List<LaptopInfo> results;
            if (keyword != null && !keyword.trim().isEmpty()) {
                results = laptopSearchService.searchByKeyword(keyword);
            } else if (minPrice != null && maxPrice != null) {
                results = laptopSearchService.searchByPriceRange(minPrice, maxPrice);
            } else {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching laptops", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/similar")
    public ResponseEntity<List<LaptopInfo>> findSimilarLaptops(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long laptopId,
            @RequestParam(defaultValue = "5") int limit) {

        try {
            List<LaptopInfo> results;
            if (description != null && !description.trim().isEmpty()) {
                results = laptopSearchService.findSimilarLaptops(description, limit);
            } else if (laptopId != null) {
                results = laptopSearchService.findSimilarLaptops(laptopId, limit);
            } else {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error finding similar laptops", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<LaptopInfo>> findByBrand(@PathVariable String brand) {
        try {
            List<LaptopInfo> results = laptopSearchService.findByBrand(brand);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error finding laptops by brand", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<LaptopInfo> findByProductId(@PathVariable String productId) {
        try {
            LaptopInfo laptop = laptopSearchService.findByProductId(productId);
            return ResponseEntity.ok(laptop);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error finding laptop by product ID", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshLaptopData() {
        try {
            laptopSearchService.refreshLaptopData();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error refreshing laptop data", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
