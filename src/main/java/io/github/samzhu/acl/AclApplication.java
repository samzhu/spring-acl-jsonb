package io.github.samzhu.acl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ACL (Access Control List) 權限控制示範專案
 *
 * 這是一個使用 Spring Boot + Spring Security + PostgreSQL JSONB 實作的 ACL 權限控制系統範例。
 *
 * 主要功能：
 * - 支援使用者 (user) 和群組 (group) 層級的權限控制
 * - 使用 PostgreSQL JSONB 儲存 ACL 條目
 * - 支援細粒度權限：read、write、delete
 * - 使用 Spring Security @PreAuthorize 進行方法級別的權限控制
 *
 * 技術堆疊：
 * - Spring Boot 4.0
 * - Spring Security 7.0
 * - Spring Data JDBC
 * - PostgreSQL (JSONB + GIN 索引)
 *
 * @see <a href="https://docs.spring.io/spring-boot/4.0/reference/">Spring Boot 官方文件</a>
 */
@SpringBootApplication
public class AclApplication {

	public static void main(String[] args) {
		SpringApplication.run(AclApplication.class, args);
	}

}
