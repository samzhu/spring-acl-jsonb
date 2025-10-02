package io.github.samzhu.acl.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 委派式權限評估器（支援多種資源類型）
 *
 * 這個實作展示如何使用策略模式 (Strategy Pattern) + 註冊表模式 (Registry Pattern)
 * 來處理不同資源類型（如 Project、Document、Task 等）的權限檢查。
 *
 * 核心設計特點：
 * - 可擴展性：新增資源類型只需建立新的 ResourcePermissionChecker @Component
 * - 自動註冊：Spring 自動注入所有 ResourcePermissionChecker beans
 * - 單一 PermissionEvaluator：符合 Spring Security 要求
 * - 支援多種資源類型，無需修改此類別
 *
 * 在 Controller 中使用：
 * <pre>
 * // 檢查特定 ID 的資源權限
 * @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'read')")
 * public Project getProject(@PathVariable Long id) {
 *     return projectService.getProjectById(id);
 * }
 *
 * // 檢查物件實例的權限
 * @PreAuthorize("hasPermission(#project, 'write')")
 * public Project updateProject(Project project) {
 *     return projectService.updateProject(project);
 * }
 * </pre>
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html#method-security-architecture">Method Security Architecture</a>
 */
@Slf4j
@Component
public class DelegatingPermissionEvaluator implements PermissionEvaluator {

    private final Map<Class<?>, ResourcePermissionChecker> checkers = new HashMap<>();

    /**
     * 建構子：自動註冊所有 ResourcePermissionChecker beans
     *
     * @param checkers 所有 ResourcePermissionChecker 實作（由 Spring 自動注入）
     */
    public DelegatingPermissionEvaluator(List<ResourcePermissionChecker> checkers) {
        checkers.forEach(checker -> {
            Class<?> supportedType = checker.getSupportedType();
            this.checkers.put(supportedType, checker);
            log.info("Registered ResourcePermissionChecker for type: {}", supportedType.getSimpleName());
        });
        log.info("DelegatingPermissionEvaluator initialized with {} resource types", this.checkers.size());
    }

    /**
     * 檢查領域物件實例的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param targetDomainObject 目標領域物件（例如：Project 實例）
     * @param permission 所需權限（例如："read"、"write"、"delete"）
     * @return 若使用者有權限則回傳 true，否則回傳 false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject == null || authentication == null) {
            log.debug("Permission denied: null target or authentication");
            return false;
        }

        Class<?> targetType = targetDomainObject.getClass();
        ResourcePermissionChecker checker = checkers.get(targetType);

        if (checker == null) {
            log.warn("No ResourcePermissionChecker found for type: {}", targetType.getName());
            return false;
        }

        boolean hasPermission = checker.hasPermission(authentication, targetDomainObject, permission.toString());
        log.debug("Permission check for {}: user={}, permission={}, result={}",
                targetType.getSimpleName(), authentication.getName(), permission, hasPermission);

        return hasPermission;
    }

    /**
     * 根據 ID 和類型檢查領域物件的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param targetId 目標物件 ID
     * @param targetType 目標類型（完整類別名稱，例如："io.github.samzhu.acl.entity.Project"）
     * @param permission 所需權限（例如："read"、"write"、"delete"）
     * @return 若使用者有權限則回傳 true，否則回傳 false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (targetId == null || authentication == null || targetType == null) {
            log.debug("Permission denied: null targetId, authentication, or targetType");
            return false;
        }

        try {
            // 從完整類別名稱載入 Class 物件
            Class<?> domainClass = Class.forName(targetType);
            ResourcePermissionChecker checker = checkers.get(domainClass);

            if (checker == null) {
                log.warn("No ResourcePermissionChecker found for type: {}", targetType);
                return false;
            }

            // 將 targetId 轉換為 Long（假設所有 ID 都是 Long 類型）
            Long id = convertToLong(targetId);
            boolean hasPermission = checker.hasPermission(authentication, id, permission.toString());

            log.debug("Permission check for {}: user={}, id={}, permission={}, result={}",
                    domainClass.getSimpleName(), authentication.getName(), id, permission, hasPermission);

            return hasPermission;

        } catch (ClassNotFoundException e) {
            log.error("Domain class not found: {}", targetType, e);
            return false;
        } catch (Exception e) {
            log.error("Error during permission check for type: {}, id: {}", targetType, targetId, e);
            return false;
        }
    }

    /**
     * 將 Serializable ID 轉換為 Long
     *
     * @param id Serializable ID
     * @return Long ID
     */
    private Long convertToLong(Serializable id) {
        if (id instanceof Long longId) {
            return longId;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(id.toString());
    }
}
