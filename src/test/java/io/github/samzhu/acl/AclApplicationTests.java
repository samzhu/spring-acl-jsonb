package io.github.samzhu.acl;

import io.github.samzhu.acl.controller.ProjectController;
import io.github.samzhu.acl.entity.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACL 示範專案 HTTP 介面測試
 *
 * 這個測試類別驗證 ACL 權限控制的各種情境，確保權限檢查正確運作。
 *
 * 測試資料說明（來自 data.sql）：
 * - 使用者：alice, bob, charlie
 * - 群組：admin, developer
 * - 專案 1：Alice 完整存取權限（讀取、寫入、刪除）
 * - 專案 2：Bob 唯讀、Alice 完整存取權限
 * - 專案 3：Admin 群組完整存取權限
 * - 專案 4：Developer 群組讀取/寫入（無刪除權限）
 * - 專案 5：混合權限（alice 讀取、developer 寫入、admin 刪除）
 * - 專案 6：私有專案（僅 alice）
 * - 專案 7, 8：所有人可讀取的公開專案
 *
 * @see <a href="https://docs.spring.io/spring-boot/4.0/reference/testing/spring-boot-applications.html">Testing Spring Boot Applications</a>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/data.sql")
@DisplayName("ACL Tests")
class AclApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    // ===== Create Tests =====

    @Test
    @DisplayName("創建項目 - 成功創建帶 ACL 的項目")
    void testCreateProject() {
        Project newProject = Project.builder()
                .name("New Test Project")
                .description("Test Description")
                .aclEntries(List.of("user:alice:read", "user:alice:write", "user:alice:delete"))
                .build();

        Project response = webTestClient.post()
                .uri("/api/projects")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newProject)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(9L);  // ID should be 9 (after test data 1-8)
        assertThat(response.getName()).isEqualTo("New Test Project");
        assertThat(response.getAclEntries()).hasSize(3);
        assertThat(response.getCreatedTime()).isNotNull();
        assertThat(response.getModifiedTime()).isNotNull();
    }

    // ===== Read Tests =====

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

        assertThat(response).isNotNull();
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

    @Test
    @DisplayName("讀取單一項目 - 項目不存在，返回 403（@PreAuthorize 先檢查權限）")
    void testGetNonExistentProject() {
        webTestClient.get()
                .uri("/api/projects/9999")
                .header("X-Username", "alice")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("讀取單一項目 - 群組權限：Admin 群組可以讀取 Admin 項目")
    void testGetProjectWithGroupPermission() {
        Project response = webTestClient.get()
                .uri("/api/projects/3")
                .header("X-Username", "alice")
                .header("X-Groups", "admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("Admin Group Project");
    }

    // ===== Search Tests =====

    @Test
    @DisplayName("搜尋項目 - Alice 搜尋有讀權限的項目")
    void testSearchProjectsAsAlice() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .header("X-Username", "alice")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertThat(content).hasSize(6);  // Projects 1,2,5,6,7,8

        List<String> names = content.stream().map(p -> (String) p.get("name")).toList();
        assertThat(names).contains(
                "Alice Full Access Project",
                "Bob Read-Only Project",
                "Mixed Permissions Project"
        );
    }

    @Test
    @DisplayName("搜尋項目 - Bob 搜尋有讀權限的項目")
    void testSearchProjectsAsBob() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .header("X-Username", "bob")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertThat(content).hasSize(3);  // Projects 2,7,8

        List<String> names = content.stream().map(p -> (String) p.get("name")).toList();
        assertThat(names).contains(
                "Bob Read-Only Project",
                "Public Read Project Alpha",
                "Public Read Project Beta"
        );
    }

    @Test
    @DisplayName("搜尋項目 - 使用名稱過濾")
    void testSearchProjectsWithNameFilter() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects?name=Alpha")
                .header("X-Username", "charlie")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("name")).isEqualTo("Public Read Project Alpha");
    }

    @Test
    @DisplayName("搜尋項目 - 支援分頁")
    void testSearchProjectsWithPagination() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects?size=3&page=0")
                .header("X-Username", "alice")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertThat(content).hasSize(3);
        assertThat(response.get("totalElements")).isEqualTo(6);
        assertThat(response.get("totalPages")).isEqualTo(2);
        assertThat(response.get("first")).isEqualTo(true);
    }

    @Test
    @DisplayName("搜尋項目 - 群組權限：Admin 群組可以看到 Admin 項目")
    void testSearchProjectsWithAdminGroup() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .header("X-Username", "alice")
                .header("X-Groups", "admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        List<String> names = content.stream().map(p -> (String) p.get("name")).toList();
        assertThat(names).contains("Admin Group Project");
    }

    // ===== Update Tests =====

    @Test
    @DisplayName("更新項目 - Alice 有寫權限，成功更新")
    void testUpdateProjectWithWritePermission() {
        Project updatedProject = Project.builder()
                .name("Updated Project Name")
                .description("Updated Description")
                .aclEntries(List.of("user:alice:read", "user:alice:write"))
                .build();

        Project response = webTestClient.put()
                .uri("/api/projects/1")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Project Name");
        assertThat(response.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("更新項目 - Bob 沒有寫權限，被拒絕 (403)")
    void testUpdateProjectWithoutWritePermission() {
        Project updatedProject = Project.builder()
                .name("Should Not Update")
                .description("Bob doesn't have write permission")
                .aclEntries(List.of())
                .build();

        webTestClient.put()
                .uri("/api/projects/2")
                .header("X-Username", "bob")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProject)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("更新項目 - Developer 群組有寫權限，成功更新")
    void testUpdateProjectWithDeveloperGroup() {
        Project updatedProject = Project.builder()
                .name("Updated by Developer")
                .description("Developer group has write permission")
                .aclEntries(List.of("group:developer:read", "group:developer:write"))
                .build();

        Project response = webTestClient.put()
                .uri("/api/projects/4")
                .header("X-Username", "alice")
                .header("X-Groups", "developer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated by Developer");
    }

    // ===== Delete Tests =====

    @Test
    @DisplayName("刪除項目 - Alice 有刪除權限，成功刪除")
    void testDeleteProjectWithDeletePermission() {
        webTestClient.delete()
                .uri("/api/projects/1")
                .header("X-Username", "alice")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("刪除項目 - Bob 沒有刪除權限，被拒絕 (403)")
    void testDeleteProjectWithoutDeletePermission() {
        webTestClient.delete()
                .uri("/api/projects/2")
                .header("X-Username", "bob")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("刪除項目 - Developer 群組沒有刪除權限，被拒絕 (403)")
    void testDeleteProjectDeveloperGroupNoDeletePermission() {
        webTestClient.delete()
                .uri("/api/projects/4")
                .header("X-Username", "alice")
                .header("X-Groups", "developer")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("刪除項目 - Admin 群組有刪除權限，成功刪除")
    void testDeleteProjectWithAdminGroup() {
        webTestClient.delete()
                .uri("/api/projects/3")
                .header("X-Username", "alice")
                .header("X-Groups", "admin")
                .exchange()
                .expectStatus().isNoContent();
    }

    // ===== Add/Remove ACL Tests =====

    @Test
    @DisplayName("加權限 - 為項目添加新的 ACL 條目")
    void testAddAclEntry() {
        ProjectController.AclEntryRequest aclRequest = new ProjectController.AclEntryRequest();
        aclRequest.setType("user");
        aclRequest.setPrincipal("charlie");
        aclRequest.setPermission("read");

        Project response = webTestClient.post()
                .uri("/api/projects/1/acl")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(aclRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getAclEntries()).contains("user:charlie:read");
    }

    @Test
    @DisplayName("移除權限 - 從項目移除 ACL 條目")
    void testRemoveAclEntry() {
        ProjectController.AclEntryRequest aclRequest = new ProjectController.AclEntryRequest();
        aclRequest.setType("user");
        aclRequest.setPrincipal("alice");
        aclRequest.setPermission("delete");

        Project response = webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/api/projects/1/acl")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(aclRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getAclEntries()).doesNotContain("user:alice:delete");
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("邊界情況 - 更新不存在的項目返回 403（@PreAuthorize 先檢查權限）")
    void testUpdateNonExistentProject() {
        Project updatedProject = Project.builder()
                .name("Does Not Exist")
                .aclEntries(List.of())
                .build();

        webTestClient.put()
                .uri("/api/projects/9999")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProject)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("邊界情況 - 匿名用戶沒有任何權限")
    void testAnonymousUserNoPermissions() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertThat(content).isEmpty();
    }

    @Test
    @DisplayName("邊界情況 - Charlie 沒有項目 6 的任何權限")
    void testCharlieNoAccessToPrivateProject() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .header("X-Username", "charlie")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        List<String> names = content.stream().map(p -> (String) p.get("name")).toList();
        assertThat(names).doesNotContain("Private Project");
    }

    @Test
    @DisplayName("邊界情況 - 多群組用戶可以訪問所有群組的項目")
    void testMultipleGroupsAccess() {
        Map<String, Object> response = webTestClient.get()
                .uri("/api/projects")
                .header("X-Username", "alice")
                .header("X-Groups", "admin,developer")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        List<String> names = content.stream().map(p -> (String) p.get("name")).toList();
        assertThat(names).contains("Admin Group Project", "Developer Group Project");
    }
}
