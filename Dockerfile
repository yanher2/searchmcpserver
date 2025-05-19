# 使用多阶段构建
# 第一阶段：构建应用
FROM maven:3.8.4-openjdk-11-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml
COPY pom.xml .

# 下载依赖
RUN mvn dependency:go-offline

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn package -DskipTests

# 第二阶段：运行环境
FROM openjdk:11-jre-slim

# 安装Chrome和其他必要的包
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    unzip \
    xvfb \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 安装ChromeDriver
RUN CHROME_VERSION=$(google-chrome --version | awk '{print $3}' | cut -d'.' -f1) \
    && CHROMEDRIVER_VERSION=$(curl -s "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_$CHROME_VERSION") \
    && wget -q "https://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip" \
    && unzip chromedriver_linux64.zip -d /usr/local/bin \
    && rm chromedriver_linux64.zip \
    && chmod +x /usr/local/bin/chromedriver

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 设置Chrome无头模式的环境变量
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROME_PATH=/usr/bin/google-chrome
ENV DISPLAY=:99

# 创建启动脚本
RUN echo '#!/bin/bash\nXvfb :99 -screen 0 1280x1024x24 &\nsleep 1\nexec java -jar app.jar' > /app/start.sh \
    && chmod +x /app/start.sh

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["/app/start.sh"]
