package io.github.samzhu.acl.controller;

import io.github.samzhu.acl.entity.Project;
import io.github.samzhu.acl.service.ProjectService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * 專案 REST API 控制器（支援 JSONB ACL 權限控制）
 *
 * 此控制器展示如何整合 PostgreSQL JSONB 和 Spring Security 實作細粒度的權限控制。
 *
 * 身份驗證方式（透過 SimpleAuthenticationFilter）：
 * - X-Username: 當前使用者名稱（例如："alice"）
 * - X-Groups: 逗號分隔的群組清單（例如："admin,developer"）
 *
 * ACL 條目格式：
 * - "user:alice:read" - 使用者 alice 有讀取權限
 * - "group:admin:write" - admin 群組有寫入權限
 * - "user:bob:delete" - 使用者 bob 有刪除權限
 *
 * API 使用範例：
 * <pre>
 * # 查詢專案列表
 * curl -H "X-Username: alice" -H "X-Groups: admin" http://localhost:8080/api/projects
 *
 * # 建立新專案
 * curl -H "X-Username: alice" -H "X-Groups: admin" -X POST http://localhost:8080/api/projects \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"Project Alpha","description":"測試專案","aclEntries":["user:alice:read","user:alice:write","user:alice:delete"]}'
 * </pre>
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller.html">Spring MVC Controller</a>
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 建立新專案
     *
     * HTTP 方法：POST /api/projects
     *
     * 請求範例：
     * <pre>
     * {
     *   "name": "Project Alpha",
     *   "description": "測試專案",
     *   "aclEntries": ["user:alice:read", "user:alice:write", "group:admin:read"]
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project createdProject = projectService.createProject(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    /**
     * 搜尋專案（支援 ACL 過濾和分頁）
     *
     * HTTP 方法：GET /api/projects?name=test&description=project&page=0&size=20
     *
     * 功能說明：
     * - 從 SecurityContext 取得當前使用者身份（由 X-Username 和 X-Groups headers 設定）
     * - 根據 ACL 條目過濾專案（檢查 read 權限）
     * - 支援選填的 name 和 description 過濾條件
     *
     * RESTful 設計原則：GET 操作永遠檢查 'read' 權限
     */
    @GetMapping
    public ResponseEntity<Page<Project>> searchProjects(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @PageableDefault(size = 20, sort = "modifiedTime") Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String[] groups = extractGroups(authentication);

        // GET 操作 = 檢查讀取權限（RESTful 原則）
        Page<Project> projects = projectService.searchProjects(
                username, groups, name, description, pageable);
        return ResponseEntity.ok(projects);
    }

    /**
     * 根據 ID 查詢單一專案
     *
     * HTTP 方法：GET /api/projects/{id}
     *
     * 權限檢查：透過 @PreAuthorize 檢查 'read' 權限
     *
     * 回應狀態碼：
     * - 200 OK: 查詢成功
     * - 403 Forbidden: 使用者沒有讀取權限
     * - 404 Not Found: 專案不存在
     */
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        try {
            Project project = projectService.getProjectById(id);
            return ResponseEntity.ok(project);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新專案
     *
     * HTTP 方法：PUT /api/projects/{id}
     *
     * 權限檢查：透過 @PreAuthorize 檢查 'write' 權限
     *
     * 請求範例：
     * <pre>
     * {
     *   "name": "更新後的專案名稱",
     *   "description": "更新後的描述",
     *   "aclEntries": ["user:alice:read", "user:alice:write"]
     * }
     * </pre>
     */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project project) {
        try {
            Project updatedProject = projectService.updateProject(id, project);
            return ResponseEntity.ok(updatedProject);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 刪除專案
     *
     * HTTP 方法：DELETE /api/projects/{id}
     *
     * 權限檢查：透過 @PreAuthorize 檢查 'delete' 權限
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 為專案新增 ACL 條目
     *
     * HTTP 方法：POST /api/projects/{id}/acl
     *
     * 請求範例：
     * <pre>
     * {
     *   "type": "user",
     *   "principal": "bob",
     *   "permission": "read"
     * }
     * </pre>
     */
    @PostMapping("/{id}/acl")
    public ResponseEntity<Project> addAclEntry(
            @PathVariable Long id,
            @RequestBody AclEntryRequest request) {
        try {
            Project updatedProject = projectService.addAclEntry(
                    id, request.getType(), request.getPrincipal(), request.getPermission());
            return ResponseEntity.ok(updatedProject);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 從專案移除 ACL 條目
     *
     * HTTP 方法：DELETE /api/projects/{id}/acl
     *
     * 請求範例：
     * <pre>
     * {
     *   "type": "user",
     *   "principal": "bob",
     *   "permission": "read"
     * }
     * </pre>
     */
    @DeleteMapping("/{id}/acl")
    public ResponseEntity<Project> removeAclEntry(
            @PathVariable Long id,
            @RequestBody AclEntryRequest request) {
        try {
            Project updatedProject = projectService.removeAclEntry(
                    id, request.getType(), request.getPrincipal(), request.getPermission());
            return ResponseEntity.ok(updatedProject);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 從 Authentication 物件中提取群組名稱
     */
    private String[] extractGroups(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    /**
     * ACL 條目請求 DTO
     *
     * 用於新增或移除專案的 ACL 權限條目。
     */
    @Data
    public static class AclEntryRequest {
        /** 類型："user" (使用者) 或 "group" (群組) */
        private String type;
        /** 主體：使用者名稱或群組名稱 */
        private String principal;
        /** 權限："read" (讀取)、"write" (寫入)、"delete" (刪除) */
        private String permission;
    }
}
