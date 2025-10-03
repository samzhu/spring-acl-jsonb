package io.github.samzhu.acl.repository;

import lombok.Builder;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 專案查詢物件（CQRS Query Object）
 *
 * 封裝專案查詢的所有參數，遵循 CQRS 和 Clean Code 精神。
 *
 * 使用場景：
 * - search(ProjectQuery) - 使用原生 SQL 查詢（完整支援 PostgreSQL JSONB）
 * - ACL 權限模式（必填）
 * - 名稱和描述過濾（可選）
 *
 * 安全特性：
 * - 輸入驗證：檢查 ACL patterns 格式
 * - SQL 注入防護：參數化查詢 + LIKE 通配符轉義
 * - 類型安全：Builder 模式確保參數正確性
 */
@Getter
@Builder
public class ProjectQuery {

    /**
     * ACL 權限模式正則表達式（類似 GCP IAM 設計）
     *
     * 格式：type:principal:permission
     *
     * 規則說明：
     * - type: 字母開頭，可包含字母、數字、底線、連字號，最多 50 字符
     * - principal: 可包含字母、數字、底線、@、點、加號、連字號，1-255 字符
     * - permission: 字母開頭，可包含字母、數字、底線、點號、連字號，最多 100 字符
     *
     * 合法範例：
     * ✅ user:alice:read
     * ✅ group:dev-team:write
     * ✅ service-account:api@example.com:delete
     * ✅ user:bob:storage.buckets.create
     * ✅ group:admin:artifactregistry.writer
     * ✅ role:viewer:compute.instances.get
     *
     * 非法範例：
     * ❌ user:alice'--:read              (principal 包含單引號)
     * ❌ :alice:read                     (type 為空)
     * ❌ user::read                      (principal 為空)
     * ❌ user:alice:project:admin        (permission 不可包含冒號)
     * ❌ 123user:alice:read              (type 必須字母開頭)
     */
    private static final Pattern ACL_PATTERN = Pattern.compile(
        "^[a-zA-Z][a-zA-Z0-9_-]{0,49}:[a-zA-Z0-9_@.+-]{1,255}:[a-zA-Z][a-zA-Z0-9_.-]{0,99}$"
    );

    /**
     * ACL 權限模式（必填）
     * 格式：type:principal:permission
     * 例如：["user:alice:read", "group:developer:storage.buckets.create"]
     */
    private final String[] aclPatterns;

    /**
     * 專案名稱過濾（可選）
     * 支援模糊查詢（LIKE）
     * 自動轉義特殊字符防止 SQL 注入
     */
    private final String name;

    /**
     * 專案描述過濾（可選）
     * 支援模糊查詢（LIKE）
     * 自動轉義特殊字符防止 SQL 注入
     */
    private final String description;

    /**
     * 工廠方法：僅 ACL 過濾
     */
    public static ProjectQuery withAcl(String[] aclPatterns) {
        return ProjectQuery.builder()
                .aclPatterns(aclPatterns)
                .build();
    }

    /**
     * 工廠方法：ACL + 名稱過濾
     */
    public static ProjectQuery withName(String[] aclPatterns, String name) {
        return ProjectQuery.builder()
                .aclPatterns(aclPatterns)
                .name(name)
                .build();
    }

    /**
     * 工廠方法：完整查詢
     */
    public static ProjectQuery of(String[] aclPatterns, String name, String description) {
        return ProjectQuery.builder()
                .aclPatterns(aclPatterns)
                .name(name)
                .description(description)
                .build();
    }

    /**
     * 檢查是否有名稱過濾條件
     */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * 檢查是否有描述過濾條件
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * 檢查是否有任何可選過濾條件
     */
    public boolean hasOptionalFilters() {
        return hasName() || hasDescription();
    }

    // ==================== 安全驗證 ====================

    /**
     * 驗證 ACL patterns 格式（使用正則表達式）
     *
     * 安全特性：
     * 1. 參數化查詢（第一層防護）：使用 NamedParameterJdbcTemplate，完全防止 SQL 注入
     * 2. 正則驗證（第二層防護）：確保業務邏輯正確性，防止無效數據
     * 3. 深度防禦：即使未來有人改用字符串拼接，正則也能提供基本保護
     *
     * 驗證邏輯：
     * - 檢查陣列是否為 null 或空
     * - 檢查每個 pattern 是否符合正則表達式
     * - 格式：type:principal:permission（冒號為分隔符，固定三段）
     *
     * @throws IllegalArgumentException 如果格式無效
     */
    public void validate() {
        if (aclPatterns == null || aclPatterns.length == 0) {
            throw new IllegalArgumentException("ACL patterns 不可為 null 或空陣列");
        }

        for (String pattern : aclPatterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                throw new IllegalArgumentException("ACL pattern 不可為 null 或空字串");
            }

            // 使用正則驗證整串格式
            if (!ACL_PATTERN.matcher(pattern).matches()) {
                throw new IllegalArgumentException(
                    String.format(
                        "無效的 ACL pattern: '%s'%n" +
                        "預期格式: type:principal:permission%n" +
                        "範例: user:alice:read, group:dev-team:storage.buckets.create%n" +
                        "請檢查：%n" +
                        "  - type 必須字母開頭，可包含字母、數字、底線、連字號（最多50字符）%n" +
                        "  - principal 可包含字母、數字、底線、@、點、加號、連字號（1-255字符）%n" +
                        "  - permission 必須字母開頭，可包含字母、數字、底線、點號、連字號（最多100字符）",
                        pattern
                    )
                );
            }
        }
    }
}
