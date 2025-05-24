package com.searchserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LaptopSearchServiceTest {

    @Mock
    private LaptopInfoRepository laptopInfoRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private JdCrawlerService crawlerService;

    @InjectMocks
    private LaptopSearchService laptopSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructorInjection() {
        assertNotNull(laptopSearchService);
        assertNotNull(laptopSearchService.getLaptopInfoRepository());
        assertNotNull(laptopSearchService.getEmbeddingService());
        assertNotNull(laptopSearchService.getCrawlerService());
    }

    // 这里可以添加更多测试方法
}
