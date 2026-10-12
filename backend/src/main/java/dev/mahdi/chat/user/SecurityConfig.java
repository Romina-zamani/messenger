package dev.mahdi.chat.user;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            UserEntity account = users.findByUsername(AccountService.normalizeUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(account.getUsername()).password(account.getPasswordHash()).roles("USER").build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF stays enabled. React obtains a token before each POST.
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login",
                "/api/signals", "/ws/signals", "/actuator/health", "/error").permitAll()
            .anyRequest().authenticated());
        http.requestCache(cache -> cache.disable());
        http.exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, error) -> response.sendError(401)));
        http.formLogin(form -> form.loginProcessingUrl("/api/auth/login")
            .successHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT))
            .failureHandler((request, response, error) -> response.sendError(401, "Invalid username or password.")));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout")
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        return http.build();
    }
}
