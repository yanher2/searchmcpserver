package com.searchserver.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebDriverConfig {
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(WebDriverConfig.class);
    @PostConstruct
    void setup() {
        try {
            // 自动下载和配置ChromeDriver
            WebDriverManager.chromedriver().setup();
            log.info("ChromeDriver has been set up successfully");
        } catch (Exception e) {
            log.error("Failed to set up ChromeDriver", e);
            throw new RuntimeException("Failed to set up ChromeDriver", e);
        }
    }
}
