# Spring Boot ACL Demo - 基於 JSONB 的權限控制系統

> 🎓 **教學專案**：本專案示範如何使用 Spring Boot 4.0 + PostgreSQL JSONB 實現靈活的權限控制系統。
> 適合學習：Spring Data JDBC、Spring Security、PostgreSQL JSONB、RESTful API 設計

## 📋 目錄

### 快速開始
- [專案概述](#專案概述)
- [快速啟動](#快速啟動)
- [第一次使用](#第一次使用)
- [API 使用範例](#api-使用範例)

### 核心概念
- [ACL 權限設計](#acl-權限設計)
- [架構設計](#架構設計)
- [核心技術](#核心技術)

### 進階主題
- [實作細節](#實作細節)
- [測試](#測試)
- [性能優化](#性能優化)
- [常見問題](#常見問題)

### 參考資源
- [專案結構](#專案結構)
- [學習資源](#學習資源)

---

## 專案概述

這是一個完整的 **ACL (Access Control List) 權限控制系統**範例，展示如何實作：

**✨ 核心功能**
- ✅ **用戶級權限**：為個別用戶設定權限（例如：`user:alice:read`）
- ✅ **群組級權限**：為用戶群組設定權限（例如：`group:admin:delete`）
- ✅ **細粒度控制**：支援 `read`、`write`、`delete` 三種權限
- ✅ **高性能查詢**：使用 PostgreSQL JSONB + GIN 索引

**🎯 適合學習的對象**
- 想了解權限系統設計的開發者
- 正在學習 Spring Boot 4.0 新特性
- 想深入了解 PostgreSQL JSONB 的使用方式
- 需要實作細粒度權限控制的專案

---

## 快速啟動

### 前置需求

```bash
# 確認版本
java --version    # 需要 Java 25+
docker --version  # 需要 Docker
```

### 三步驟啟動

```bash
# 1️⃣ 啟動 PostgreSQL
docker-compose up -d

# 2️⃣ 執行應用程式
./gradlew bootRun

# 3️⃣ 驗證服務
curl http://localhost:8080/actuator/health
# 回應：{"status":"UP"}
```

---

## 第一次使用

### Step 1: 創建你的第一個專案

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "X-Username: alice" \
  -d '{
    "name": "我的第一個專案",
    "description": "學習 ACL 權限控制",
    "aclEntries": [
      "user:alice:read",
      "user:alice:write",
      "user:alice:delete"
    ]
  }'
```

**回應（201 Created）：**
```json
{
  "id": 1,
  "name": "我的第一個專案",
  "aclEntries": ["user:alice:read", "user:alice:write", "user:alice:delete"],
  "createdTime": "2025-10-02T10:30:00Z"
}
```

### Step 2: 查詢專案清單

```bash
# Alice 查詢（會看到剛建立的專案）
curl "http://localhost:8080/api/projects" \
  -H "X-Username: alice"

# Bob 查詢（看不到 Alice 的專案）
curl "http://localhost:8080/api/projects" \
  -H "X-Username: bob"
```

### Step 3: 理解權限檢查

```bash
# ✅ Alice 可以讀取（有 read 權限）
curl "http://localhost:8080/api/projects/1" \
  -H "X-Username: alice"
# 回應：200 OK + 專案資料

# ❌ Bob 無法讀取（沒有權限）
curl "http://localhost:8080/api/projects/1" \
  -H "X-Username: bob"
# 回應：403 Forbidden
```

---

## ACL 權限設計

### 權限格式說明

每個 ACL 條目由三部分組成：`type:principal:permission`

```
┌────────┬──────────┬────────────┐
│  類型  │   主體   │    權限    │
├────────┼──────────┼────────────┤
│ user   │  alice   │   read     │  ← user:alice:read
│ group  │  admin   │   delete   │  ← group:admin:delete
└────────┴──────────┴────────────┘
```

**參數說明：**
- **type**：`user`（使用者）或 `group`（群組）
- **principal**：使用者名稱或群組名稱
- **permission**：`read`（讀）、`write`（寫）、`delete`（刪）

### 實際範例

```json
{
  "name": "團隊專案",
  "aclEntries": [
    "user:alice:read",          // Alice 可以讀取
    "user:alice:write",         // Alice 可以寫入
    "user:alice:delete",        // Alice 可以刪除
    "group:developer:read",     // developer 群組可以讀取
    "group:developer:write",    // developer 群組可以寫入
    "group:admin:delete"        // admin 群組可以刪除
  ]
}
```

### 權限檢查邏輯

當 Bob（屬於 developer 群組）嘗試讀取專案時：

```
1️⃣ 系統提取 Bob 的身份
   - username: "bob"
   - groups: ["developer"]

2️⃣ 建立 ACL 匹配模式（檢查 read 權限）
   - "user:bob:read"
   - "group:developer:read"

3️⃣ PostgreSQL 查詢
   SELECT * FROM project
   WHERE acl_entries ?| ARRAY['user:bob:read', 'group:developer:read']

4️⃣ 結果
   ✅ 專案的 aclEntries 包含 "group:developer:read"
   ✅ Bob 可以讀取此專案
```

---

## API 使用範例

### 基本操作

#### 1. 創建專案
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "X-Username: alice" \
  -d '{
    "name": "專案名稱",
    "description": "專案描述",
    "aclEntries": ["user:alice:read", "user:alice:write"]
  }'
```

#### 2. 查詢專案清單
```bash
# 基本查詢
curl "http://localhost:8080/api/projects" \
  -H "X-Username: alice"

# 名稱過濾
curl "http://localhost:8080/api/projects?name=測試" \
  -H "X-Username: alice"

# 分頁查詢
curl "http://localhost:8080/api/projects?page=0&size=10" \
  -H "X-Username: alice"
```

#### 3. 查詢單一專案
```bash
curl "http://localhost:8080/api/projects/1" \
  -H "X-Username: alice"
```

#### 4. 更新專案
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Content-Type: application/json" \
  -H "X-Username: alice" \
  -d '{
    "name": "更新後的名稱",
    "description": "更新後的描述",
    "aclEntries": ["user:alice:read", "user:alice:write"]
  }'
```

#### 5. 刪除專案
```bash
curl -X DELETE http://localhost:8080/api/projects/1 \
  -H "X-Username: alice"
```

### 進階操作

#### 管理 ACL 權限

**新增權限：**
```bash
curl -X POST http://localhost:8080/api/projects/1/acl \
  -H "Content-Type: application/json" \
  -H "X-Username: alice" \
  -d '{
    "type": "user",
    "principal": "bob",
    "permission": "read"
  }'
```

**移除權限：**
```bash
curl -X DELETE http://localhost:8080/api/projects/1/acl \
  -H "Content-Type: application/json" \
  -H "X-Username: alice" \
  -d '{
    "type": "user",
    "principal": "bob",
    "permission": "read"
  }'
```

### RESTful 設計原則

| HTTP 方法 | 端點 | 權限檢查 | 說明 |
|-----------|------|---------|------|
| GET | `/api/projects` | `read` | 查詢清單（自動過濾） |
| GET | `/api/projects/{id}` | `read` | 查詢單一項目 |
| POST | `/api/projects` | - | 創建新專案（無需權限） |
| PUT | `/api/projects/{id}` | `write` | 更新專案 |
| DELETE | `/api/projects/{id}` | `delete` | 刪除專案 |

---

## 架構設計

### 整體架構

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                       │
│  ProjectController (@PreAuthorize 權限檢查)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Service Layer                          │
│  ProjectService (業務邏輯處理)                          │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│               Repository Layer                          │
│  ProjectRepository + Custom ACL 查詢                    │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              PostgreSQL Database                        │
│  JSONB acl_entries + GIN 索引                           │
└─────────────────────────────────────────────────────────┘
```

### 資料庫 Schema

```sql
CREATE TABLE project (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    acl_entries     JSONB DEFAULT '[]'::jsonb,  -- ACL 權限清單
    created_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    modified_time   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER DEFAULT 1
);

-- GIN 索引：優化 JSONB 查詢性能（10-100倍提升）
CREATE INDEX idx_project_acl ON project
USING GIN (acl_entries jsonb_path_ops);
```

---

## 核心技術

| 技術 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.0-M3 | 應用框架 |
| Spring Data JDBC | - | 輕量級資料存取 |
| Spring Security | 6.x | 權限控制 |
| PostgreSQL | Latest | 資料庫 |
| Java | 25 | 程式語言 |
| Docker Compose | - | 開發環境 |

---

## 實作細節

### 1. 自訂 JSONB 查詢

**使用 NamedParameterJdbcTemplate 實現 ACL 查詢**

```java
@Override
public Page<Project> search(ProjectQuery query, Pageable pageable) {
    // 驗證 ACL patterns 格式
    query.validate();

    // 使用 ??| 運算符（JDBC 轉義語法）
    String sql = """
        SELECT * FROM project
        WHERE acl_entries ??| CAST(:patterns AS text[])
        LIMIT :limit OFFSET :offset
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("patterns", query.getAclPatterns())
        .addValue("limit", pageable.getPageSize())
        .addValue("offset", pageable.getOffset());

    return jdbcTemplate.query(sql, params, projectRowMapper);
}
```

**重要技巧：JDBC 轉義語法**

| PostgreSQL | JDBC 寫法 | 說明 |
|-----------|----------|------|
| `?|` | `??|` | 包含任一元素（OR） |
| `?&` | `??&` | 包含所有元素（AND） |
| `?` | `??` | 鍵存在檢查 |

> 💡 **為什麼要用 `??`？**
> 在 JDBC 中，`?` 是參數占位符。PostgreSQL JDBC 驅動使用 `??` 轉義語法，執行時會轉換為單個 `?`。

參考：[PostgreSQL JDBC 官方文檔](https://jdbc.postgresql.org/documentation/query/)

### 2. Spring Security 權限檢查

**可擴展的權限檢查設計（Strategy Pattern）**

```java
// 1️⃣ 定義資源權限檢查器介面
public interface ResourcePermissionChecker {
    boolean hasPermission(Authentication auth, Long id, String permission);
    Class<?> getSupportedType();
}

// 2️⃣ 實作 Project 專屬的權限檢查器
@Component
public class ProjectPermissionEvaluator implements ResourcePermissionChecker {

    @Override
    public Class<?> getSupportedType() {
        return Project.class;  // 註冊支援 Project 類型
    }

    @Override
    public boolean hasPermission(Authentication auth, Long projectId, String permission) {
        String username = auth.getName();
        String[] groups = extractGroups(auth);
        String[] patterns = buildAclPatterns(username, groups, permission);
        return projectRepository.hasPermission(projectId, patterns);
    }
}

// 3️⃣ 主要的 PermissionEvaluator（自動註冊所有 Checker）
@Component
public class DelegatingPermissionEvaluator implements PermissionEvaluator {

    private final Map<Class<?>, ResourcePermissionChecker> checkers;

    // Spring 自動注入所有 ResourcePermissionChecker
    public DelegatingPermissionEvaluator(List<ResourcePermissionChecker> checkers) {
        this.checkers = checkers.stream()
            .collect(Collectors.toMap(
                ResourcePermissionChecker::getSupportedType,
                checker -> checker
            ));
    }
}
```

**在 Service 層使用 @PreAuthorize**

```java
@Service
public class ProjectService {

    // 讀取權限檢查
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'read')")
    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow();
    }

    // 寫入權限檢查
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'write')")
    public Project updateProject(Long id, Project project) {
        // ...
    }

    // 刪除權限檢查
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'delete')")
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
```

### 3. 擴展新資源類型

**範例：新增 Document 資源**

```java
// 步驟 1：創建 DocumentPermissionChecker（完全獨立）
@Component
public class DocumentPermissionChecker implements ResourcePermissionChecker {

    @Override
    public Class<?> getSupportedType() {
        return Document.class;  // 自動註冊到系統
    }

    @Override
    public boolean hasPermission(Authentication auth, Long docId, String permission) {
        // Document 專屬的權限檢查邏輯
        return documentRepository.hasPermission(docId, buildAclPatterns(...));
    }
}

// 步驟 2：在 Service 使用（無需修改其他代碼）
@Service
public class DocumentService {

    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Document', 'read')")
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }
}
```

**設計優勢：**
- ✅ 新增資源只需添加一個 `@Component`
- ✅ 無需修改 `DelegatingPermissionEvaluator`
- ✅ 符合開閉原則（對擴展開放，對修改關閉）

---

## 測試

### 執行測試

```bash
# 執行所有測試
./gradlew test

# 查看測試報告
open build/reports/tests/test/index.html
```

### 測試覆蓋範圍

- ✅ 23 個整合測試
- ✅ 100% 測試成功率
- ✅ 使用 Testcontainers（真實 PostgreSQL）
- ✅ 使用 WebTestClient（Spring Boot 4.0 推薦）

### 測試範例

```java
@Test
@DisplayName("讀取單一項目 - Alice 有讀權限，成功讀取")
void testGetProjectWithReadPermission() {
    Project response = webTestClient.get()
            .uri("/api/projects/1")
            .header("X-Username", "alice")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Project.class)
            .returnResult()
            .getResponseBody();

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("Alice Full Access Project");
}

@Test
@DisplayName("讀取單一項目 - Bob 沒有讀權限，被拒絕 (403)")
void testGetProjectWithoutReadPermission() {
    webTestClient.get()
            .uri("/api/projects/1")
            .header("X-Username", "bob")
            .exchange()
            .expectStatus().isForbidden();
}
```

---

## 性能優化

### GIN 索引說明

```sql
-- 使用 jsonb_path_ops 獲得更好性能
CREATE INDEX idx_project_acl ON project
USING GIN (acl_entries jsonb_path_ops);
```

**性能提升：**
- ✅ 索引大小較 `jsonb_ops` 小 30%
- ✅ 查詢速度提升 10-100 倍（vs 無索引）
- ✅ 單次查詢僅需 ~0.011ms（3 筆資料測試）

**兩種 GIN 索引比較：**

| 索引類型 | 支援運算符 | 索引大小 | 查詢速度 | 適用場景 |
|---------|----------|---------|---------|---------|
| `jsonb_ops` | 全部 | 較大 | 快 | 需要多種運算符 |
| `jsonb_path_ops` | `@>`, `?|`, `?&` | **小 30%** | **更快** | ✅ ACL 查詢（本專案） |

### 已實作的優化措施

| 優化項 | 實作方式 | 效果 |
|--------|---------|------|
| **GIN 索引** | `jsonb_path_ops` | 查詢速度提升 10-100 倍 |
| **參數化查詢** | `NamedParameterJdbcTemplate` | SQL 執行計畫重用 |
| **分頁查詢** | `LIMIT :limit OFFSET :offset` | 減少資料傳輸量 |
| **LIKE 轉義** | `sanitizeLikePattern()` | 防止 SQL 注入 |

---

## 常見問題

### Q1: 為什麼使用 JSONB 而不是關聯表？

**A:** 各有優缺點，選擇取決於需求：

**JSONB 方案（本專案）：**
- ✅ **彈性高**：ACL 條目數量不固定
- ✅ **開發簡單**：避免複雜的 JOIN 查詢
- ✅ **查詢效率**：配合 GIN 索引，性能優秀
- ❌ **限制**：不適合需要複雜 ACL 分析的場景

**關聯表方案：**
- ✅ **標準化**：符合資料庫設計規範
- ✅ **適合複雜查詢**：如統計分析
- ❌ **開發複雜**：需要處理多表 JOIN
- ❌ **性能開銷**：大量 ACL 條目時查詢變慢

### Q2: 如何從 Spring Boot 3.x 升級到 4.0？

**A:** 主要變更：

1. **測試工具變更**
   ```java
   // ❌ Spring Boot 3.x
   @Autowired
   private TestRestTemplate restTemplate;

   // ✅ Spring Boot 4.0
   @Autowired
   private WebTestClient webTestClient;
   ```

2. **依賴調整**
   ```gradle
   // 新增 WebFlux（用於 WebTestClient）
   testImplementation 'org.springframework.boot:spring-boot-starter-webflux'
   ```

3. **Java 版本**
   - 最低要求：Java 17+
   - 建議使用：Java 21+ 或 Java 25

參考：[Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

#### ⚠️ 已知問題：WebTestClient vs RestTestClient

**問題描述：**
- 本專案使用 **WebTestClient** 進行測試（需要 `spring-boot-starter-webflux` 依賴）
- 雖然專案使用 Spring MVC（非響應式），但為了測試而引入了 WebFlux 依賴
- Spring Framework 7.0.0-M8 已推出 **RestTestClient**（專為非響應式應用設計）
- **但 Spring Boot 4.0.0-M3 尚未自動配置 RestTestClient**

**RestTestClient 資訊：**
- Package: `org.springframework.test.web.servlet.client.RestTestClient`
- 已在 Spring Framework 7.0.0-M8+ 可用（[PR #34428](https://github.com/spring-projects/spring-framework/pull/34428)）
- 文件：[RestTestClient Documentation](https://docs.spring.io/spring-framework/reference/7.0/testing/resttestclient.html)
- Spring Boot 支援追蹤：[Issue #47335](https://github.com/spring-projects/spring-boot/issues/47335)

**後續跟進：**
- [ ] 追蹤 Spring Boot 4.0.0-RC1 或正式版是否支援 RestTestClient 自動配置
- [ ] 若支援，遷移測試程式碼從 WebTestClient 到 RestTestClient
- [ ] 移除 `spring-boot-starter-webflux` 依賴（目前必須保留）

### Q3: 正式環境部署注意事項

**A:** 生產環境檢查清單：

- ⚠️ **移除 SimpleAuthenticationFilter**
  → 改用真實認證（OAuth2、JWT、SAML）

- ⚠️ **修改 schema.sql**
  → 移除 `DROP TABLE`，使用 Liquibase 或 Flyway

- ⚠️ **調整連線池配置**
  → 根據負載調整 HikariCP 設定

- ⚠️ **啟用 CSRF 保護**
  → SecurityConfig 中啟用 `.csrf().enable()`

- ⚠️ **設定正確的 CORS**
  → 限制允許的來源網域

### Q4: 如何除錯權限檢查？

**A:** 啟用 Spring Security 除錯模式：

```yaml
# application.properties
logging.level.org.springframework.security=DEBUG
```

這會輸出詳細的權限檢查日誌：
```
Permission check for Project: user=alice, id=1, permission=read, result=true
```

### Q5: 如何擴展更多權限類型？

**A:** 修改 ACL 格式即可：

```java
// 支援欄位級別權限
"user:alice:read:field_salary"
"user:alice:write:field_name"

// 支援時間限制的權限（需要額外邏輯驗證）
"user:alice:read:2025-12-31"

// 支援操作級別權限
"user:alice:approve"
"user:alice:reject"
```

---

## 專案結構

```
src/
├── main/
│   ├── java/io/github/samzhu/acl/
│   │   ├── AclApplication.java                    # 應用程式入口
│   │   ├── config/
│   │   │   ├── JdbcConfiguration.java             # JDBC 型別轉換器
│   │   │   └── SecurityConfig.java                # Security 配置
│   │   ├── controller/
│   │   │   └── ProjectController.java             # REST API
│   │   ├── entity/
│   │   │   └── Project.java                       # 實體類別
│   │   ├── repository/
│   │   │   ├── ProjectRepository.java             # 標準 Repository
│   │   │   ├── ProjectRepositoryCustom.java       # 自訂查詢介面
│   │   │   ├── ProjectRepositoryCustomImpl.java   # ACL 查詢實作
│   │   │   └── ProjectQuery.java                  # CQRS 查詢物件
│   │   ├── security/
│   │   │   ├── SimpleAuthenticationFilter.java    # 認證過濾器（示範用）
│   │   │   ├── ResourcePermissionChecker.java     # 權限檢查器介面
│   │   │   ├── DelegatingPermissionEvaluator.java # 主要權限評估器
│   │   │   └── ProjectPermissionEvaluator.java    # Project 權限檢查器
│   │   └── service/
│   │       └── ProjectService.java                # 業務邏輯層
│   └── resources/
│       ├── application.properties                 # 應用配置
│       └── schema.sql                             # 資料庫 Schema
└── test/
    ├── java/io/github/samzhu/acl/
    │   ├── AclApplicationTests.java               # 整合測試（23 個測試）
    │   └── TestcontainersConfiguration.java       # Testcontainers 配置
    └── resources/
        └── data.sql                               # 測試資料
```

---

## 學習資源

### 官方文檔

**Spring Framework**
- [Spring Boot 4.0](https://docs.spring.io/spring-boot/4.0/reference/)
- [Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/4.0/jdbc.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html)

**PostgreSQL**
- [JSONB 資料型態](https://www.postgresql.org/docs/current/datatype-json.html)
- [GIN 索引](https://www.postgresql.org/docs/current/gin.html)
- [PostgreSQL JDBC](https://jdbc.postgresql.org/documentation/query/)

### 推薦學習順序

**初學者路徑：**
1. 先跑通 [第一次使用](#第一次使用) 的範例
2. 理解 [ACL 權限設計](#acl-權限設計) 的基本概念
3. 閱讀 [API 使用範例](#api-使用範例) 熟悉 API
4. 查看測試程式碼了解各種情境

**進階開發者路徑：**
1. 研究 [實作細節](#實作細節) 了解技術選型
2. 學習如何擴展新資源類型
3. 深入研究 PostgreSQL JSONB 查詢優化
4. 參考 [性能優化](#性能優化) 改善查詢效率

### 關鍵技術文章

**PostgreSQL JDBC 轉義語法**
- [Official: Issuing a Query and Processing the Result](https://jdbc.postgresql.org/documentation/query/)

**Spring Security 擴展設計**
- [Method Security Architecture](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html#method-security-architecture)
- [Custom PermissionEvaluator](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html#_the_permissionevaluator_interface)

---

## 授權

MIT License

---

**Made with ❤️ for learning Spring Boot & PostgreSQL**
