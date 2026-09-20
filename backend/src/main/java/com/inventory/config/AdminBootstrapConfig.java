package com.inventory.config;

import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

// Since /api/auth/register is now ADMIN-only (see SecurityConfig), a brand-new deployment
// with an empty users table would otherwise have no way to create its first admin account
// without touching the database directly. This runner closes that gap safely: it only ever
// acts when there is no ADMIN in the database yet, and only if ADMIN_BOOTSTRAP_* env vars are
// explicitly set. On a system that already has an admin (like the current live deployment),
// this is a complete no-op.
@Configuration
public class AdminBootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    public CommandLineRunner seedInitialAdmin(UserRepository userRepository,
                                               PasswordEncoder passwordEncoder,
                                               @Value("${app.bootstrap.admin-username:}") String username,
                                               @Value("${app.bootstrap.admin-email:}") String email,
                                               @Value("${app.bootstrap.admin-password:}") String password) {
        return args -> {
            boolean hasAdmin = userRepository.findByDeletedFalse().stream()
                    .anyMatch(u -> u.getRole() == Role.ADMIN);
            if (hasAdmin) {
                return;
            }
            if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                log.warn("No ADMIN user exists yet and ADMIN_BOOTSTRAP_USERNAME/EMAIL/PASSWORD are not set. "
                        + "Set those environment variables to have the application create the first "
                        + "admin account automatically on startup.");
                return;
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            admin.setDeleted(false);
            userRepository.save(admin);
            log.info("Created initial ADMIN user '{}' from bootstrap configuration.", username);
        };
    }
}
