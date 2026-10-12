package dev.mahdi.chat.user;

import java.security.Principal;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;
    private final UserRepository users;

    public AuthController(AccountService accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView register(@RequestBody Registration registration) {
        return view(accounts.register(registration.username(), registration.password()));
    }

    @GetMapping("/me")
    public AccountView me(Principal principal) {
        return view(users.findByUsername(principal.getName()).orElseThrow());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> invalid(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("detail", error.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicate() {
        return ResponseEntity.status(409).body(Map.of("detail", "That username is already taken."));
    }

    private AccountView view(UserEntity user) { return new AccountView(user.getId(), user.getUsername()); }
    public record Registration(String username, String password) {
        // Spring may log request objects at DEBUG level; never include the password.
        @Override public String toString() { return "Registration[credentials redacted]"; }
    }
    public record AccountView(Long id, String username) {}
}
