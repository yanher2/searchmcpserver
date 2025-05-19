package com.searchserver.repository;

import com.searchserver.model.LaptopInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LaptopInfoRepository extends JpaRepository<LaptopInfo, Long> {

    Optional<LaptopInfo> findByProductId(String productId);

    List<LaptopInfo> findByBrand(String brand);

    List<LaptopInfo> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query(value = "SELECT * FROM laptop_info ORDER BY embedding <-> :embedding LIMIT :limit", nativeQuery = true)
    List<LaptopInfo> findSimilarLaptops(@Param("embedding") double[] embedding, @Param("limit") int limit);

    @Query(value = "SELECT * FROM laptop_info WHERE brand LIKE %:keyword% OR model LIKE %:keyword% OR title LIKE %:keyword% OR description LIKE %:keyword%", nativeQuery = true)
    List<LaptopInfo> searchByKeyword(@Param("keyword") String keyword);
}
