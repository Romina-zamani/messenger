package dev.mahdi.chat;

import dev.mahdi.chat.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class AuthIntegrationTest {
  @Value("${local.server.port}") int port;
  @Autowired UserRepository users;

  @Test void registrationLoginSessionAndLogout() throws Exception {
    var client = new AuthTestClient(port);
    assertEquals(401, client.get("/api/auth/me").statusCode());
    var registration = client.register("learner");
    assertEquals(201, registration.statusCode(), registration.body());
    assertFalse(registration.body().contains("password"));
    String hash = users.findByUsername("learner").orElseThrow().getPasswordHash();
    assertNotEquals("LearningJava123!", hash);
    assertTrue(hash.startsWith("$2"));
    assertEquals(401, client.login("learner", "wrong-password").statusCode());
    assertEquals(204, client.login("learner", "LearningJava123!").statusCode());
    assertTrue(client.get("/api/auth/me").body().contains("learner"));
    assertEquals(403, client.post("/api/auth/logout", "", false, false).statusCode());
    assertEquals(200, client.get("/api/auth/me").statusCode());
    assertEquals(204, client.post("/api/auth/logout", "", false, true).statusCode());
    assertEquals(401, client.get("/api/auth/me").statusCode());
  }

  @Test void registrationDoesNotExposePasswordsInDiagnosticText() {
    var registration = new dev.mahdi.chat.user.AuthController.Registration("learner", "private-test-password");
    assertFalse(registration.toString().contains("private-test-password"));
  }

  @Test void rejectsDuplicateInvalidAndUnprotectedRegistration() throws Exception {
    var client = new AuthTestClient(port);
    assertEquals(201, client.register("unique_user").statusCode());
    assertEquals(409, client.register("unique_user").statusCode());
    assertEquals(400, client.register("bad name").statusCode());
    assertEquals(400, client.post("/api/auth/register", "{\"username\":\"shortpass\",\"password\":\"short\"}", true, true).statusCode());
    assertEquals(403, client.post("/api/auth/register", "{}", true, false).statusCode());
    assertEquals(403, client.post("/api/auth/login", "username=unique_user&password=LearningJava123!", false, false).statusCode());
    assertEquals(400, client.post("/api/auth/register", "{\"username\":\"longpass\",\"password\":\"" + "a".repeat(73) + "\"}", true, true).statusCode());
  }

  @Test void normalizesUsernamesAndRotatesSessionOnLogin() throws Exception {
    var client = new AuthTestClient(port);
    assertEquals(201, client.register(" Mixed_Name ").statusCode());
    assertEquals(409, client.register("mixed_name").statusCode());
    var cookies = (java.net.CookieManager) client.http.cookieHandler().orElseThrow();
    String before = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    assertEquals(204, client.login("MIXED_NAME", "LearningJava123!").statusCode());
    String after = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    assertNotEquals(before, after);
    assertTrue(client.get("/api/auth/me").body().contains("mixed_name"));
  }
}
