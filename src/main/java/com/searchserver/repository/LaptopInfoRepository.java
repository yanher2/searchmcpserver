package com.searchserver.repository;

import com.searchserver.model.LaptopInfo;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LaptopInfoRepository {

    // 基本CRUD操作
    LaptopInfo save(LaptopInfo laptopInfo);
    
    Optional<LaptopInfo> findById(Long id);
    
    List<LaptopInfo> findAll();
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
    
    // 原有的查询方法
    Optional<LaptopInfo> findByProductId(String productId);

    List<LaptopInfo> findByBrand(String brand);

    List<LaptopInfo> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 向量相似度搜索
    List<LaptopInfo> findSimilarLaptops(double[] embedding, int limit);

    // 关键词搜索
    List<LaptopInfo> searchByKeyword(String keyword);
    
    // 批量操作
    List<LaptopInfo> saveAll(List<LaptopInfo> laptops);
    
    void deleteAll();

    /**
     * 检查存储库连接状态
     * @return 如果连接正常返回true，否则返回false
     */
    boolean isConnected();
}
