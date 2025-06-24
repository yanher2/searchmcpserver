package com.searchserver.service;

import com.searchserver.model.LaptopInfo;
import com.searchserver.repository.LaptopInfoRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JdCrawlerService {
    @Resource
    private LaptopInfoRepository laptopInfoRepository;
    @Resource
    private EmbeddingService embeddingService;

    @Value("${app.jd.base-url}")
    private String baseUrl;

    @Value("${app.jd.user-agent}")
    private String userAgent;
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(JdCrawlerService.class);
    @Scheduled(fixedDelayString = "${app.jd.crawl-interval}")
    public void scheduledCrawl() {
        try {
            log.info("Starting scheduled crawl of JD second-hand laptops");
            List<LaptopInfo> laptops = crawlJdSecondHandLaptops();
            log.info("Crawled {} laptops", laptops.size());
        } catch (Exception e) {
            log.error("Error during scheduled crawl", e);
        }
    }

    public List<LaptopInfo> crawlJdSecondHandLaptops() {
        List<LaptopInfo> results = new ArrayList<>();
        try {
            // 访问京东二手笔记本电脑页面
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent(userAgent)
                    .timeout(10000)
                    .get();

            // 提取商品列表
            Elements items = doc.select(".gl-item");

            for (Element item : items) {
                try {
                    String productId = item.attr("data-sku");

                    // 检查是否已存在
                    Optional<LaptopInfo> existingLaptop = laptopInfoRepository.findByProductId(productId);
                    if (existingLaptop.isPresent()) {
                        continue;
                    }

                    String title = item.select(".p-name em").text();
                    String imageUrl = item.select(".p-img img").attr("data-lazy-img");
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    }

                    String productUrl = item.select(".p-img a").attr("href");
                    if (productUrl.startsWith("//")) {
                        productUrl = "https:" + productUrl;
                    }

                    // 提取价格
                    String priceText = item.select(".p-price i").text().replace("￥", "");
                    BigDecimal price = new BigDecimal(priceText);

                    // 创建基本的笔记本信息
                    LaptopInfo laptop = new LaptopInfo();
                    laptop.setProductId(productId);
                    laptop.setTitle(title);
                    laptop.setImageUrl(imageUrl);
                    laptop.setProductUrl(productUrl);
                    laptop.setPrice(price);

                    // 获取详细信息
                    enrichLaptopDetails(laptop);

                    // 生成嵌入向量
                    double[] embedding = embeddingService.generateEmbedding(
                            laptop.getTitle() + " " +
                            laptop.getDescription() + " " +
                            laptop.getBrand() + " " +
                            laptop.getModel() + " " +
                            laptop.getProcessorInfo() + " " +
                            laptop.getMemoryInfo() + " " +
                            laptop.getStorageInfo() + " " +
                            laptop.getDisplayInfo()
                    );
                    laptop.setEmbedding(embedding);

                    // 保存到数据库
                    laptopInfoRepository.save(laptop);
                    results.add(laptop);

                } catch (Exception e) {
                    log.error("Error processing laptop item", e);
                }
            }

        } catch (Exception e) {
            log.error("Error crawling JD second-hand laptops", e);
        }

        return results;
    }

    private void enrichLaptopDetails(LaptopInfo laptop) {
        try {
            // 访问商品详情页
            Document doc = Jsoup.connect(laptop.getProductUrl())
                    .userAgent(userAgent)
                    .timeout(10000)
                    .get();

            // 提取品牌
            Elements brandElements = doc.select(".p-parameter-list li:contains(品牌)");
            if (!brandElements.isEmpty()) {
                String brandText = brandElements.first().text();
                laptop.setBrand(extractValue(brandText, "品牌"));
            }

            // 提取型号
            Elements modelElements = doc.select(".p-parameter-list li:contains(型号)");
            if (!modelElements.isEmpty()) {
                String modelText = modelElements.first().text();
                laptop.setModel(extractValue(modelText, "型号"));
            }

            // 提取处理器信息
            Elements cpuElements = doc.select(".p-parameter-list li:contains(处理器)");
            if (!cpuElements.isEmpty()) {
                String cpuText = cpuElements.first().text();
                laptop.setProcessorInfo(extractValue(cpuText, "处理器"));
            }

            // 提取内存信息
            Elements memoryElements = doc.select(".p-parameter-list li:contains(内存容量)");
            if (!memoryElements.isEmpty()) {
                String memoryText = memoryElements.first().text();
                laptop.setMemoryInfo(extractValue(memoryText, "内存容量"));
            }

            // 提取存储信息
            Elements storageElements = doc.select(".p-parameter-list li:contains(硬盘容量)");
            if (!storageElements.isEmpty()) {
                String storageText = storageElements.first().text();
                laptop.setStorageInfo(extractValue(storageText, "硬盘容量"));
            }

            // 提取显示屏信息
            Elements displayElements = doc.select(".p-parameter-list li:contains(屏幕尺寸)");
            if (!displayElements.isEmpty()) {
                String displayText = displayElements.first().text();
                laptop.setDisplayInfo(extractValue(displayText, "屏幕尺寸"));
            }

            // 提取成色信息（二手特有）
            Elements conditionElements = doc.select(".p-parameter-list li:contains(成色)");
            if (!conditionElements.isEmpty()) {
                String conditionText = conditionElements.first().text();
                laptop.setConditionGrade(extractValue(conditionText, "成色"));
            }

            // 提取商品描述
            Elements descElements = doc.select(".p-parameter-list");
            if (!descElements.isEmpty()) {
                laptop.setDescription(descElements.text());
            }

            // 提取卖家信息
            Elements sellerElements = doc.select(".seller-infor a");
            if (!sellerElements.isEmpty()) {
                laptop.setSellerName(sellerElements.text());
            }

        } catch (Exception e) {
            log.error("Error enriching laptop details for {}", laptop.getProductId(), e);
        }
    }

    private String extractValue(String text, String label) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(label + "[:：]\\s*(.+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return text.replace(label, "").trim();
    }
}