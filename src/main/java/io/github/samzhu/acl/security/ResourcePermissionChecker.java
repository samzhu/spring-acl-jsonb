package io.github.samzhu.acl.security;

import org.springframework.security.core.Authentication;

/**
 * 資源權限檢查器介面
 *
 * 這個介面允許針對不同領域物件進行可擴展的權限檢查。
 * 每種領域物件類型（如 Project、Document、Task）都應該有自己的實作。
 *
 * 實作範例：
 * <pre>
 * @Component
 * public class ProjectPermissionChecker implements ResourcePermissionChecker {
 *     @Override
 *     public Class<?> getSupportedType() {
 *         return Project.class;
 *     }
 *
 *     @Override
 *     public boolean hasPermission(Authentication auth, Object target, String permission) {
 *         // Project 專屬的權限檢查邏輯
 *         return checkProjectPermission(auth, (Project) target, permission);
 *     }
 *
 *     @Override
 *     public boolean hasPermission(Authentication auth, Long targetId, String permission) {
 *         // 根據 ID 進行 Project 專屬的權限檢查
 *         return checkProjectPermissionById(auth, targetId, permission);
 *     }
 * }
 * </pre>
 */
public interface ResourcePermissionChecker {

    /**
     * 檢查領域物件實例的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param target 目標領域物件（例如：Project、Document）
     * @param permission 所需權限（例如："read"、"write"、"delete"）
     * @return 若使用者有權限則回傳 true，否則回傳 false
     */
    boolean hasPermission(Authentication authentication, Object target, String permission);

    /**
     * 根據 ID 檢查領域物件的權限
     *
     * @param authentication 當前使用者的身份驗證資訊
     * @param targetId 目標物件 ID
     * @param permission 所需權限（例如："read"、"write"、"delete"）
     * @return 若使用者有權限則回傳 true，否則回傳 false
     */
    boolean hasPermission(Authentication authentication, Long targetId, String permission);

    /**
     * 取得此檢查器支援的領域物件類型
     *
     * @return 領域物件的 Class（例如：Project.class、Document.class）
     */
    Class<?> getSupportedType();
}
