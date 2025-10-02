package io.github.samzhu.acl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 專案實體類別 (使用 Spring Data JDBC)
 *
 * 這個類別展示如何使用 PostgreSQL JSONB 欄位儲存 ACL 權限清單。
 *
 * ACL 條目格式：
 * - 格式：["user:sam:read", "group:admin:write", "user:bob:delete"]
 * - type: "user" (使用者) 或 "group" (群組)
 * - principal: 使用者名稱或群組名稱
 * - permission: "read" (讀)、"write" (寫)、"delete" (刪除)
 *
 * 設計重點：
 * - 使用 @Table 指定資料表名稱
 * - 使用 @Id 標記主鍵
 * - 使用 @Version 實作樂觀鎖定 (Optimistic Locking)
 * - aclEntries 欄位對應到資料庫的 JSONB 類型
 *
 * @see <a href="https://docs.spring.io/spring-data/relational/reference/4.0/jdbc/mapping.html">Spring Data JDBC Mapping</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("project")
public class Project {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("acl_entries")
    @Builder.Default
    private List<String> aclEntries = new ArrayList<>();

    @Column("created_time")
    private Instant createdTime;

    @Column("modified_time")
    private Instant modifiedTime;

    @Version
    @Column("version")
    private Integer version;
}
