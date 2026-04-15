package com.luisa.wechat.chat.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MysqlCharsetMigrationConfig {

    private static final List<String> MYSQL_UTF8MB4_MIGRATIONS = List.of(
            "ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE friendships CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE friend_requests CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE chat_groups CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE group_members CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE conversation_states CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    );

    @Bean
    ApplicationRunner mysqlCharsetMigrationRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            if (!isMySql(dataSource)) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS friend_requests (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友申请主键',
                        requester_id BIGINT NOT NULL COMMENT '申请发起人用户 ID',
                        recipient_id BIGINT NOT NULL COMMENT '申请接收人用户 ID',
                        created_at TIMESTAMP NOT NULL COMMENT '申请创建时间',
                        CONSTRAINT uk_friend_requests_requester_recipient UNIQUE (requester_id, recipient_id),
                        CONSTRAINT fk_friend_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id),
                        CONSTRAINT fk_friend_requests_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='好友申请表'
                    """);
            ensureFriendRemarkColumn(jdbcTemplate, dataSource);
            for (String sql : MYSQL_UTF8MB4_MIGRATIONS) {
                jdbcTemplate.execute(sql);
            }
        };
    }

    private void ensureFriendRemarkColumn(JdbcTemplate jdbcTemplate, DataSource dataSource) throws SQLException {
        if (hasColumn(dataSource, "friendships", "friend_remark")) {
            jdbcTemplate.execute("""
                    ALTER TABLE friendships
                    MODIFY COLUMN friend_remark VARCHAR(200) NULL COMMENT '拥有者对好友的备注'
                    """);
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE friendships
                ADD COLUMN friend_remark VARCHAR(200) NULL COMMENT '拥有者对好友的备注' AFTER friend_id
                """);
    }

    private boolean hasColumn(DataSource dataSource, String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (var columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                return columns.next();
            }
        }
    }

    private boolean isMySql(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return metadata.getDatabaseProductName() != null
                    && metadata.getDatabaseProductName().toLowerCase().contains("mysql");
        }
    }
}
