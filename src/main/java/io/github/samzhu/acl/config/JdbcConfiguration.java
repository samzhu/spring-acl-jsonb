package io.github.samzhu.acl.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Data JDBC 自訂型別轉換器配置
 *
 * 這個配置類別展示如何處理 Java 類型與 PostgreSQL JSONB 之間的轉換。
 *
 * 主要功能：
 * - 將 Java List<String> 轉換為 PostgreSQL JSONB（寫入資料庫時）
 * - 將 PostgreSQL JSONB 轉換為 Java List<String>（從資料庫讀取時）
 *
 * 使用場景：
 * - Project.aclEntries (List<String>) ← → database.acl_entries (JSONB)
 *
 * @see <a href="https://docs.spring.io/spring-data/relational/reference/4.0/jdbc/mapping.html#jdbc.custom-converters">Custom Converters</a>
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
                new ListToPGobjectConverter(),
                new PGobjectToListConverter()
        );
    }

    /**
     * 寫入轉換器：List<String> → PostgreSQL JSONB
     *
     * 當 Spring Data JDBC 儲存實體時，會自動呼叫此轉換器。
     */
    @WritingConverter
    static class ListToPGobjectConverter implements Converter<List<String>, PGobject> {
        @Override
        public PGobject convert(List<String> source) {
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            try {
                pgObject.setValue(objectMapper.writeValueAsString(source));
                return pgObject;
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException("Failed to convert List<String> to JSONB", e);
            }
        }
    }

    /**
     * 讀取轉換器：PostgreSQL JSONB → List<String>
     *
     * 當 Spring Data JDBC 查詢實體時，會自動呼叫此轉換器。
     */
    @ReadingConverter
    static class PGobjectToListConverter implements Converter<PGobject, List<String>> {
        @Override
        public List<String> convert(PGobject source) {
            if (source == null || source.getValue() == null) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(source.getValue(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert JSONB to List<String>", e);
            }
        }
    }
}
