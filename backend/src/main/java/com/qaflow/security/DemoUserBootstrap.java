package com.qaflow.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "qaflow.bootstrap.enabled", havingValue = "true")
public class DemoUserBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserBootstrap.class);

    private final BootstrapProperties properties;
    private final Environment environment;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventRecorder auditEventRecorder;
    private final Clock clock;

    public DemoUserBootstrap(
            BootstrapProperties properties,
            Environment environment,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuditEventRecorder auditEventRecorder,
            Clock clock) {
        this.properties = properties;
        this.environment = environment;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventRecorder = auditEventRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException("Demo user bootstrap is forbidden in the prod profile");
        }

        List<BootstrapAccount> accounts = List.of(
                new BootstrapAccount(
                        properties.adminUsername(), properties.adminDisplayName(), properties.adminPassword(), Role.ADMIN),
                new BootstrapAccount(
                        properties.operatorUsername(), properties.operatorDisplayName(), properties.operatorPassword(), Role.OPERATOR),
                new BootstrapAccount(
                        properties.inspectorUsername(), properties.inspectorDisplayName(), properties.inspectorPassword(), Role.QUALITY_INSPECTOR));
        accounts.forEach(BootstrapAccount::validate);

        Instant now = Instant.now(clock);
        int created = 0;
        for (BootstrapAccount account : accounts) {
            String normalizedUsername = account.username().trim().toUpperCase(Locale.ROOT);
            if (userAccountRepository.existsByUsernameNormalized(normalizedUsername)) {
                continue;
            }
            userAccountRepository.save(UserAccount.create(
                    account.username().trim(),
                    account.displayName().trim(),
                    passwordEncoder.encode(account.password()),
                    account.role(),
                    true,
                    now,
                    null));
            created++;
        }
        userAccountRepository.flush();
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                null,
                null,
                null,
                "DEMO_USERS_BOOTSTRAPPED",
                "SYSTEM",
                "startup",
                AuditOutcome.SUCCESS,
                Map.of("createdUsers", created),
                null,
                "bootstrap-users"));
        LOGGER.info("Demo user bootstrap completed createdUsers={}", created);
    }

    private record BootstrapAccount(String username, String displayName, String password, Role role) {

        private void validate() {
            if (username == null || username.trim().length() < 3 || username.trim().length() > 100) {
                throw new IllegalStateException("Every bootstrap username must contain between 3 and 100 characters");
            }
            if (displayName == null || displayName.trim().isEmpty() || displayName.trim().length() > 150) {
                throw new IllegalStateException("Every bootstrap display name must contain between 1 and 150 characters");
            }
            if (password == null || password.length() < 8 || password.length() > 128) {
                throw new IllegalStateException("Every bootstrap password must contain between 8 and 128 characters");
            }
        }
    }
}
