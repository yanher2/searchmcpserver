# 京东二手笔记本电脑信息检索系统

基于Spring Boot和LangChain4j的MCP服务器，用于检索京东二手笔记本电脑信息。该系统可以通过IDEA中的CodeBuddy插件调用腾讯代码云大语言模型来使用。

## 功能特点

- 自动爬取京东二手笔记本电脑信息
- 使用向量数据库进行相似度搜索
- 支持关键词搜索和价格范围筛选
- 提供MCP服务器接口，支持大语言模型调用
- 定期更新数据，保持信息时效性

## 系统要求

- JDK 11或更高版本
- PostgreSQL 14或更高版本（需要pgvector扩展）
- Chrome浏览器（用于Selenium爬虫）
- IDEA + CodeBuddy插件

## 快速开始

### 1. 数据库设置

1. 安装PostgreSQL数据库
2. 安装pgvector扩展：
```sql
CREATE EXTENSION vector;
```
3. 执行初始化脚本：
```bash
psql -U postgres -d your_database_name -f src/main/resources/db/init.sql
```

### 2. 配置应用程序

1. 修改`application.yml`中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database_name
    username: your_username
    password: your_password
```

### 3. 运行应用程序

```bash
mvn spring-boot:run
```

## 使用方法

### 通过IDEA + CodeBuddy使用

1. 在IDEA中安装CodeBuddy插件
2. 确保应用程序正在运行
3. 在IDEA中使用自然语言描述你的需求，例如：
   - "查找价格在5000-8000之间的ThinkPad笔记本"
   - "找一台性能好的游戏本，预算12000以内"
   - "查找成色最好的MacBook Pro"

### MCP服务器API

系统提供以下工具API：

1. **搜索笔记本电脑**
```json
{
  "name": "search_laptops",
  "arguments": {
    "keyword": "ThinkPad",
    "minPrice": 5000,
    "maxPrice": 8000
  }
}
```

2. **查找相似笔记本**
```json
{
  "name": "find_similar_laptops",
  "arguments": {
    "description": "需要一台性能强劲的游戏本，预算8000左右",
    "limit": 5
  }
}
```

3. **获取笔记本详情**
```json
{
  "name": "get_laptop_by_id",
  "arguments": {
    "productId": "12345678"
  }
}
```

4. **刷新数据**
```json
{
  "name": "refresh_laptop_data",
  "arguments": {}
}
```

## 项目结构

```
src/main/java/com/mathserver/
├── config/                 # 配置类
├── controller/            # REST API控制器
├── model/                # 数据模型
├── repository/           # 数据访问层
├── service/             # 业务逻辑层
└── mcp/                 # MCP服务器实现
```

## 注意事项

1. 数据爬取遵循京东的robots.txt规则
2. 建议定期更新数据以保持信息时效性
3. 首次运行时需要等待数据爬取完成
4. 确保数据库中已启用pgvector扩展
5. 如遇到Chrome驱动问题，请确保Chrome浏览器版本与驱动版本匹配

## 常见问题

1. Q: 无法连接数据库？
   A: 检查PostgreSQL服务是否运行，以及配置文件中的连接信息是否正确。

2. Q: 爬虫无法工作？
   A: 确保已安装Chrome浏览器，并且WebDriver配置正确。

3. Q: 向量搜索不准确？
   A: 检查pgvector扩展是否正确安装，以及向量维度是否匹配（384维）。

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目。在提交代码前，请确保：

1. 代码符合项目的编码规范
2. 添加了适当的测试
3. 更新了相关文档

## 许可证

本项目采用MIT许可证。详见LICENSE文件。
