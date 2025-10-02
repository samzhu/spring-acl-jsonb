package io.github.samzhu.acl.repository;

import io.github.samzhu.acl.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 自定義 Repository 介面
 *
 * 提供 ACL 相關的自定義查詢方法，遵循 Clean Code 和 CQRS 精神。
 *
 * 使用 NamedParameterJdbcTemplate 實現，完整支援 PostgreSQL JSONB 運算符。
 */
public interface ProjectRepositoryCustom {

    /**
     * 搜尋專案（基於查詢物件）
     *
     * 使用 ProjectQuery 查詢物件封裝查詢參數
     *
     * 範例：
     * <pre>
     * ProjectQuery query = ProjectQuery.builder()
     *     .aclPatterns(aclPatterns)
     *     .name("Project Name")
     *     .description("Description")
     *     .build();
     * Page&lt;Project&gt; result = repository.search(query, pageable);
     * </pre>
     *
     * 特點：
     * - ✅ 完整支援 PostgreSQL JSONB 運算符（?|, ?&, @>）
     * - ✅ 輸入驗證（ACL patterns 格式檢查）
     * - ✅ SQL 注入防護（參數化查詢 + LIKE 通配符轉義）
     * - ✅ GIN 索引優化（ACL JSONB 查詢）
     *
     * @param query 查詢物件（封裝 ACL patterns、name、description）
     * @param pageable 分頁參數
     * @return 分頁結果
     */
    Page<Project> search(ProjectQuery query, Pageable pageable);

    /**
     * 檢查專案是否有權限
     *
     * @param projectId 專案 ID
     * @param aclPatterns ACL 權限模式陣列
     * @return 是否有權限
     */
    boolean hasPermission(Long projectId, String[] aclPatterns);
}
