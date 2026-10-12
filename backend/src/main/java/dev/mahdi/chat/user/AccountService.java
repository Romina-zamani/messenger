package dev.mahdi.chat.user;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AccountService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public UserEntity register(String username, String password) {
        String name = normalizeUsername(username);
        if (!name.matches("[a-z0-9_]{3,30}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use 3 to 30 letters, numbers, or underscores for your username.");
        }
        if (password == null || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use at least 12 characters and at most 72 UTF-8 bytes for your password.");
        }
        if (users.findByUsername(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
        }
        return users.saveAndFlush(new UserEntity(name, passwords.encode(password), Instant.now()));
    }
}
