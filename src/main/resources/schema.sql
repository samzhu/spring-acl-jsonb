-- 每次重啟時清除舊資料
DROP TABLE IF EXISTS project CASCADE;

CREATE TABLE IF NOT EXISTS project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    acl_entries JSONB DEFAULT '[]'::jsonb,
    created_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- 建立 GIN 索引優化 JSONB 查詢
-- 使用 jsonb_path_ops 運算子類別可獲得更好的性能和更小的索引
-- 參考：https://www.postgresql.org/docs/current/datatype-json.html#JSON-INDEXING
CREATE INDEX IF NOT EXISTS idx_project_acl ON project USING GIN (acl_entries jsonb_path_ops);