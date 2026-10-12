package dev.mahdi.chat.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {
    @Autowired private UserRepository users;
    @Autowired private EntityManager entityManager;

    @Test
    void savesAndReloadsAccountFromMigratedSchema() {
        Instant createdAt = Instant.parse("2026-01-01T12:00:00Z");
        UserEntity saved = users.saveAndFlush(
            new UserEntity("romina", "test-only-encoded-hash", createdAt));
        Long id = saved.getId();
        assertNotNull(id);
        entityManager.clear();

        UserEntity reloaded = users.findByUsername("romina").orElseThrow();
        assertEquals(id, reloaded.getId());
        assertEquals("romina", reloaded.getUsername());
        assertEquals("test-only-encoded-hash", reloaded.getPasswordHash());
        assertEquals(createdAt, reloaded.getCreatedAt());
    }

    @Test
    void returnsEmptyWhenUsernameDoesNotExist() {
        assertTrue(users.findByUsername("missing-user").isEmpty());
    }

    @Test
    void databaseRejectsDuplicateUsername() {
        users.saveAndFlush(new UserEntity("duplicate", "test-hash-one", Instant.now()));
        assertThrows(DataIntegrityViolationException.class, () ->
            users.saveAndFlush(new UserEntity("duplicate", "test-hash-two", Instant.now())));
    }
}
