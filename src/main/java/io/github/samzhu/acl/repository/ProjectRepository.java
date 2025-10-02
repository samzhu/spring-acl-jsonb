package io.github.samzhu.acl.repository;

import io.github.samzhu.acl.entity.Project;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * 專案資料存取介面
 *
 * 繼承 Spring Data 標準介面，並擴充自訂的 ACL 查詢功能。
 *
 * 繼承介面說明：
 * 1. CrudRepository - 提供基本的 CRUD 操作
 *    - save(S entity) - 儲存實體
 *    - findById(ID id) - 根據 ID 查詢
 *    - existsById(ID id) - 檢查是否存在
 *    - findAll() - 查詢全部
 *    - deleteById(ID id) - 根據 ID 刪除
 *    - delete(T entity) - 刪除實體
 *    - count() - 計算總數
 *
 * 2. PagingAndSortingRepository - 提供分頁與排序功能
 *    - findAll(Pageable pageable) - 分頁查詢
 *    - findAll(Sort sort) - 排序查詢
 *
 * 3. ProjectRepositoryCustom - 提供自訂的 ACL 查詢方法
 *    - search(...) - ACL 權限過濾查詢
 *    - hasPermission(...) - 權限檢查
 *
 * @see <a href="https://docs.spring.io/spring-data/relational/reference/4.0/repositories/definition.html">定義 Repository 介面</a>
 */
public interface ProjectRepository extends
        CrudRepository<Project, Long>,
        PagingAndSortingRepository<Project, Long>,
        ProjectRepositoryCustom {

    // Spring Data JDBC 會自動實作所有繼承的介面方法
    // 不需要撰寫實作程式碼

}
