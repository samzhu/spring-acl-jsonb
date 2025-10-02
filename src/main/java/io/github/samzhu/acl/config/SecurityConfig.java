package io.github.samzhu.acl.config;

import io.github.samzhu.acl.security.SimpleAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（支援 ACL 權限控制）
 *
 * 這個配置類別展示如何整合自訂的 PermissionEvaluator 實現方法級別的權限控制。
 *
 * 技術堆疊：
 * - Spring Boot 4.0 + Spring Security 7.0
 * - 使用 @EnableMethodSecurity 啟用方法安全（取代舊版 @EnableGlobalMethodSecurity）
 *
 * @EnableMethodSecurity(prePostEnabled = true) 說明：
 * - 啟用方法級別權限控制，支援 @PreAuthorize 和 @PostAuthorize 註解
 * - prePostEnabled: 啟用 Pre/Post 註解，支援 SpEL 表達式（如 hasPermission(#id, 'Project', 'read')）
 * - 相較於舊版 @EnableGlobalMethodSecurity，這是 Spring Security 6+ 推薦做法
 *
 * 核心設計：
 * - DelegatingPermissionEvaluator: 委派式權限評估器，支援多種資源類型
 * - hasPermission(): 在 @PreAuthorize 中使用的權限檢查方法
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html">Method Security</a>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SimpleAuthenticationFilter authenticationFilter;

    /**
     * 配置 HTTP 安全規則
     *
     * 注意：此為示範專案，因此允許所有請求（permitAll）。
     * 生產環境中應啟用適當的身份驗證機制（如 OAuth2、JWT 等）。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置方法安全表達式處理器
     *
     * 整合 DelegatingPermissionEvaluator，啟用 @PreAuthorize("hasPermission(...)") 語法。
     *
     * 工作原理：
     * 1. DelegatingPermissionEvaluator 自動發現並註冊所有 ResourcePermissionChecker beans
     * 2. 每個資源類型（如 Project、Document）可以有自己的權限檢查器
     * 3. @PreAuthorize 會呼叫對應的權限檢查器進行權限驗證
     *
     * 使用範例：
     * <pre>
     * // 檢查特定 ID 的資源權限
     * @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'read')")
     * public Project getProject(Long id) { ... }
     *
     * // 檢查物件實例的權限
     * @PreAuthorize("hasPermission(#project, 'write')")
     * public Project updateProject(Project project) { ... }
     *
     * // 支援不同資源類型
     * @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Document', 'read')")
     * public Document getDocument(Long id) { ... }
     * </pre>
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }
}
