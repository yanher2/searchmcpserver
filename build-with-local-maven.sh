#!/bin/bash

# 确保脚本在错误时退出
set -e

# 显示使用说明
echo "构建使用本地Maven仓库的Docker镜像"
echo "====================================="

# 获取Maven本地仓库路径
if [ -z "$MAVEN_REPO" ]; then
    # 尝试从Maven配置中获取仓库路径
    if [ -f "$HOME/.m2/settings.xml" ]; then
        MAVEN_REPO=$(grep -A 1 "<localRepository>" "$HOME/.m2/settings.xml" | grep -v "<localRepository>" | sed 's/[<>/]//g' | tr -d ' ')
    fi
    
    # 如果没有找到，使用默认路径
    if [ -z "$MAVEN_REPO" ]; then
        MAVEN_REPO="$HOME/.m2/repository"
    fi
fi

echo "使用Maven仓库路径: $MAVEN_REPO"

# 构建第一阶段镜像（使用本地Maven仓库）
echo "正在构建构建阶段镜像..."
docker build -t java-search-mcp-server-build:latest -f dockerbuild1 . \
    --build-arg MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository" \
    --mount type=bind,source="$MAVEN_REPO",target=/root/.m2/repository

# 构建第二阶段镜像
echo "正在构建运行环境镜像..."
docker build -t java-search-mcp-server:latest .

echo "构建完成!"
echo "构建镜像: java-search-mcp-server-build:latest"
echo "运行镜像: java-search-mcp-server:latest"
