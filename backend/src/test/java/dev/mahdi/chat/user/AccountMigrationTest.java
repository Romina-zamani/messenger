package dev.mahdi.chat.user;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountMigrationTest {
    @Test void upgradingKeepsLegacyMessagesUnassigned() throws Exception {
        String url = "jdbc:h2:mem:legacy-upgrade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("2").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO messages(nickname, content, sent_at) VALUES ('romina', 'old message', CURRENT_TIMESTAMP)");
            Flyway.configure().dataSource(url, "sa", "").load().migrate();
            statement.executeUpdate("INSERT INTO users(username, password_hash, created_at) VALUES ('romina', 'test-hash', CURRENT_TIMESTAMP)");
            try (var rows = statement.executeQuery("SELECT nickname, content, sender_id FROM messages")) {
                assertTrue(rows.next());
                assertEquals("romina", rows.getString("nickname"));
                assertEquals("old message", rows.getString("content"));
                assertNull(rows.getObject("sender_id"));
                assertFalse(rows.next());
            }
        }
    }
}
