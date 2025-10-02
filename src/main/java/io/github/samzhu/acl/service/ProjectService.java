package io.github.samzhu.acl.service;

import io.github.samzhu.acl.entity.Project;
import io.github.samzhu.acl.repository.ProjectQuery;
import io.github.samzhu.acl.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 專案服務層
 *
 * 提供專案的業務邏輯處理，整合 ACL 權限控制。
 *
 * 設計重點：
 * - 使用 @Transactional(readOnly = true) 作為類別級別預設值，提升讀取效能
 * - 使用 @PreAuthorize 進行方法級別的權限控制
 * - 寫入操作覆寫 @Transactional 以啟用寫入權限
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html">Method Security</a>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * 建立新專案
     */
    @Transactional
    public Project createProject(Project project) {
        Instant now = Instant.now();
        project.setCreatedTime(now);
        project.setModifiedTime(now);
        project.setVersion(null);  // Let Spring Data JDBC use DEFAULT 0

        if (project.getAclEntries() == null) {
            project.setAclEntries(new ArrayList<>());
        }

        return projectRepository.save(project);
    }

    /**
     * 根據 ID 查詢專案
     *
     * 透過 @PreAuthorize 檢查讀取權限
     */
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'read')")
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));
    }

    /**
     * 搜尋專案（含 ACL 過濾）
     *
     * 支援選填的名稱和描述過濾條件。
     * 遵循 RESTful 原則：GET 操作永遠檢查 'read' 權限。
     * 使用 CQRS 模式的 ProjectQuery 查詢物件。
     */
    public Page<Project> searchProjects(String username, String[] groups,
                                        String name, String description, Pageable pageable) {
        // RESTful 原則：GET 操作永遠檢查讀取權限
        String[] aclPatterns = buildAclPatterns(username, groups, "read");

        // 建構 ProjectQuery 查詢物件（CQRS 模式）
        ProjectQuery query = ProjectQuery.builder()
                .aclPatterns(aclPatterns)
                .name(name)
                .description(description)
                .build();

        return projectRepository.search(query, pageable);
    }

    /**
     * 更新現有專案
     *
     * 透過 @PreAuthorize 檢查寫入權限
     */
    @Transactional
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'write')")
    public Project updateProject(Long id, Project updatedProject) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));

        existingProject.setName(updatedProject.getName());
        existingProject.setDescription(updatedProject.getDescription());
        existingProject.setAclEntries(updatedProject.getAclEntries());
        existingProject.setModifiedTime(Instant.now());

        return projectRepository.save(existingProject);
    }

    /**
     * 刪除專案
     *
     * 透過 @PreAuthorize 檢查刪除權限
     */
    @Transactional
    @PreAuthorize("hasPermission(#id, 'io.github.samzhu.acl.entity.Project', 'delete')")
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    /**
     * 為專案新增 ACL 權限條目
     */
    @Transactional
    public Project addAclEntry(Long projectId, String type, String principal, String permission) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        String aclEntry = String.format("%s:%s:%s", type, principal, permission);

        if (!project.getAclEntries().contains(aclEntry)) {
            project.getAclEntries().add(aclEntry);
            project.setModifiedTime(Instant.now());
            return projectRepository.save(project);
        }

        return project;
    }

    /**
     * 從專案移除 ACL 權限條目
     */
    @Transactional
    public Project removeAclEntry(Long projectId, String type, String principal, String permission) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        String aclEntry = String.format("%s:%s:%s", type, principal, permission);

        if (project.getAclEntries().remove(aclEntry)) {
            project.setModifiedTime(Instant.now());
            return projectRepository.save(project);
        }

        return project;
    }

    /**
     * 從使用者名稱、群組和權限建構 ACL 模式陣列
     */
    private String[] buildAclPatterns(String username, String[] groups, String permission) {
        List<String> patterns = new ArrayList<>();

        patterns.add(String.format("user:%s:%s", username, permission));

        if (groups != null && groups.length > 0) {
            for (String group : groups) {
                if (group != null && !group.isEmpty()) {
                    patterns.add(String.format("group:%s:%s", group, permission));
                }
            }
        }

        return patterns.toArray(new String[0]);
    }
}
