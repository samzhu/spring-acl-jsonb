package io.github.samzhu.acl.repository;

import lombok.Builder;
import lombok.Getter;

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
     * ACL 權限模式（必填）
     * 格式：type:principal:permission
     * 例如：["user:alice:read", "group:developer:read"]
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
     * 驗證 ACL patterns 格式
     *
     * 格式：type:principal:permission
     * - type: user 或 group
     * - principal: 用戶名或群組名（不含特殊字符）
     * - permission: read, write, delete
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

            String[] parts = pattern.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                        "無效的 ACL pattern 格式: " + pattern + "。預期格式: type:principal:permission"
                );
            }

            String type = parts[0];
            String principal = parts[1];
            String permission = parts[2];

            // 驗證 type
            if (!type.equals("user") && !type.equals("group")) {
                throw new IllegalArgumentException(
                        "無效的 ACL type: " + type + "。必須是 'user' 或 'group'"
                );
            }

            // 驗證 principal（防止 SQL 注入）
            if (!isValidPrincipal(principal)) {
                throw new IllegalArgumentException(
                        "無效的 principal: " + principal + "。包含無效字符"
                );
            }

            // 驗證 permission
            if (!permission.equals("read") && !permission.equals("write") && !permission.equals("delete")) {
                throw new IllegalArgumentException(
                        "無效的 permission: " + permission + "。必須是 'read', 'write', 或 'delete'"
                );
            }
        }
    }

    /**
     * 驗證 principal 是否安全（僅允許字母、數字、底線、連字號）
     */
    private boolean isValidPrincipal(String principal) {
        if (principal == null || principal.trim().isEmpty()) {
            return false;
        }
        // 只允許字母、數字、底線、連字號
        return principal.matches("^[a-zA-Z0-9_-]+$");
    }
}
