package io.github.samzhu.acl.security;

import io.github.samzhu.acl.entity.Project;
import io.github.samzhu.acl.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 專案權限檢查器
 *
 * 實作 ResourcePermissionChecker 介面，專門處理 Project 領域物件的權限檢查。
 *
 * 工作原理：
 * - 此檢查器會自動註冊到 DelegatingPermissionEvaluator
 * - 處理所有關於 Project 資源的權限檢查
 * - 根據 ACL 條目判斷使用者是否有權限
 *
 * 在 Controller 中使用：
 * <pre>
 * // 根據 ID 檢查權限
 * @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'read')")
 * public Project getProject(Long id) { ... }
 *
 * // 根據物件實例檢查權限
 * @PreAuthorize("hasPermission(#project, 'write')")
 * public Project updateProject(Project project) { ... }
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements ResourcePermissionChecker {

    private final ProjectRepository projectRepository;

    /**
     * 回傳此檢查器支援的領域物件類型
     *
     * @return Project.class
     */
    @Override
    public Class<?> getSupportedType() {
        return Project.class;
    }

    /**
     * 檢查 Project 實例的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param target 目標 Project 實例
     * @param permission 所需權限 (read/write/delete)
     * @return 若有權限則回傳 true，否則回傳 false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object target, String permission) {
        if (authentication == null || target == null) {
            return false;
        }

        // 對於 Project 物件，提取 ID 並委派給 hasPermission(id, permission)
        if (target instanceof Project project) {
            if (project.getId() == null) {
                // 新專案尚未儲存 - 允許建立
                return true;
            }
            return hasPermission(authentication, project.getId(), permission);
        }

        return false;
    }

    /**
     * 根據 ID 檢查 Project 的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param projectId 目標 Project ID
     * @param permission 所需權限 (read/write/delete)
     * @return 若有權限則回傳 true，否則回傳 false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Long projectId, String permission) {
        if (authentication == null || projectId == null) {
            return false;
        }

        // 從 Authentication 中提取使用者名稱和群組
        String username = authentication.getName();
        String[] groups = extractGroups(authentication);

        // 建構 ACL 模式並檢查權限
        String[] aclPatterns = buildAclPatterns(username, groups, permission);
        return projectRepository.hasPermission(projectId, aclPatterns);
    }

    /**
     * 從使用者名稱、群組和權限建構 ACL 模式陣列
     *
     * 範例：buildAclPatterns("sam", ["admin", "dev"], "read")
     *   => ["user:sam:read", "group:admin:read", "group:dev:read"]
     */
    private String[] buildAclPatterns(String username, String[] groups, String permission) {
        List<String> patterns = new ArrayList<>();

        // 新增使用者模式
        patterns.add(String.format("user:%s:%s", username, permission));

        // 新增群組模式
        if (groups != null && groups.length > 0) {
            for (String group : groups) {
                if (group != null && !group.isEmpty()) {
                    patterns.add(String.format("group:%s:%s", group, permission));
                }
            }
        }

        return patterns.toArray(new String[0]);
    }

    /**
     * 從 Authentication authorities 中提取群組名稱
     */
    private String[] extractGroups(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))  // 群組通常有 ROLE_ 前綴
                .map(auth -> auth.substring(5))  // 移除 "ROLE_" 前綴
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
