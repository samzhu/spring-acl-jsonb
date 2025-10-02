package io.github.samzhu.acl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 簡易身份驗證過濾器（僅供示範用途）
 *
 * 這個過濾器從 HTTP headers 或查詢參數中讀取使用者資訊，用於示範 ACL 權限控制。
 *
 * 支援的參數：
 * - X-Username 或 username: 當前使用者名稱
 * - X-Groups 或 groups: 逗號分隔的群組清單
 *
 * 使用範例：
 * <pre>
 * # 使用 HTTP headers
 * curl -H "X-Username: sam" -H "X-Groups: admin,developer" http://localhost:8080/api/projects/1
 *
 * # 使用查詢參數
 * curl "http://localhost:8080/api/projects/1?username=sam&groups=admin,developer"
 * </pre>
 *
 * 重要提醒：
 * 生產環境中應該使用適當的身份驗證機制（如 OAuth2、JWT、SAML 等），
 * 此過濾器僅用於開發和示範目的。
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/architecture.html">Authentication Architecture</a>
 */
@Component
public class SimpleAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 嘗試從 header 或參數取得使用者名稱
        String username = getHeaderOrParameter(request, "X-Username", "username");
        String groupsStr = getHeaderOrParameter(request, "X-Groups", "groups");

        // 如果沒有提供使用者名稱，預設為 anonymous
        if (username == null || username.isEmpty()) {
            username = "anonymous";
        }

        // 解析群組並轉換為 Spring Security 的 authorities
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (groupsStr != null && !groupsStr.isEmpty()) {
            String[] groups = groupsStr.split(",");
            for (String group : groups) {
                String trimmed = group.trim();
                if (!trimmed.isEmpty()) {
                    // 加上 ROLE_ 前綴（Spring Security 慣例）
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmed));
                }
            }
        }

        // 建立 Authentication token 並設定到 SecurityContext
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * 優先從 header 取值，若沒有則從查詢參數取值
     */
    private String getHeaderOrParameter(HttpServletRequest request, String headerName, String paramName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isEmpty()) {
            value = request.getParameter(paramName);
        }
        return value;
    }
}
