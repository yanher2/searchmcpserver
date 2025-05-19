-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建笔记本信息表
CREATE TABLE IF NOT EXISTS laptop_info (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    product_id VARCHAR(50) UNIQUE,
    image_url VARCHAR(500),
    product_url VARCHAR(500),
    price DECIMAL(10, 2),
    original_price DECIMAL(10, 2),
    brand VARCHAR(100),
    model VARCHAR(100),
    processor_info VARCHAR(200),
    memory_info VARCHAR(100),
    storage_info VARCHAR(100),
    display_info VARCHAR(200),
    condition_grade VARCHAR(50),
    seller_name VARCHAR(100),
    seller_rating DECIMAL(3, 1),
    embedding vector(384),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建向量索引
CREATE INDEX IF NOT EXISTS laptop_embedding_idx ON laptop_info USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 创建品牌索引
CREATE INDEX IF NOT EXISTS laptop_brand_idx ON laptop_info(brand);

-- 创建价格索引
CREATE INDEX IF NOT EXISTS laptop_price_idx ON laptop_info(price);

-- 创建更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER laptop_info_updated_at
    BEFORE UPDATE ON laptop_info
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 创建相似性搜索函数
CREATE OR REPLACE FUNCTION find_similar_laptops(query_embedding vector(384), limit_count INTEGER)
RETURNS TABLE (
    id BIGINT,
    title VARCHAR(255),
    similarity FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        l.id,
        l.title,
        1 - (l.embedding <=> query_embedding) as similarity
    FROM laptop_info l
    WHERE l.embedding IS NOT NULL
    ORDER BY l.embedding <=> query_embedding
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- 添加全文搜索支持
ALTER TABLE laptop_info ADD COLUMN IF NOT EXISTS search_text tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(brand, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(model, '')), 'A')
    ) STORED;

CREATE INDEX IF NOT EXISTS laptop_search_idx ON laptop_info USING GIN (search_text);

-- 创建全文搜索函数
CREATE OR REPLACE FUNCTION search_laptops(search_query TEXT)
RETURNS TABLE (
    id BIGINT,
    title VARCHAR(255),
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        l.id,
        l.title,
        ts_rank(l.search_text, to_tsquery('english', search_query)) as rank
    FROM laptop_info l
    WHERE l.search_text @@ to_tsquery('english', search_query)
    ORDER BY rank DESC;
END;
$$ LANGUAGE plpgsql;

-- 创建价格范围搜索函数
CREATE OR REPLACE FUNCTION search_laptops_by_price_range(min_price DECIMAL, max_price DECIMAL)
RETURNS TABLE (
    id BIGINT,
    title VARCHAR(255),
    price DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        l.id,
        l.title,
        l.price
    FROM laptop_info l
    WHERE l.price BETWEEN min_price AND max_price
    ORDER BY l.price ASC;
END;
$$ LANGUAGE plpgsql;
