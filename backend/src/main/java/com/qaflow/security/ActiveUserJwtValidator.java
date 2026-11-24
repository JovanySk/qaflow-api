package com.qaflow.security;

import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_USER = new OAuth2Error(
            "invalid_token",
            "The user authorization is no longer valid.",
            null);

    private final UserAccountRepository userAccountRepository;

    public ActiveUserJwtValidator(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Long userId = parseSubject(token.getSubject());
        Object authVersionClaim = token.getClaim("authVersion");
        Long authVersion = authVersionClaim instanceof Number number ? number.longValue() : null;
        String role = token.getClaimAsString("role");
        if (userId == null || authVersion == null || role == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_USER);
        }

        return userAccountRepository.findById(userId)
                .filter(UserAccount::active)
                .filter(user -> user.authVersion() == authVersion)
                .filter(user -> user.role().name().equals(role))
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_USER));
    }

    private Long parseSubject(String subject) {
        try {
            long value = Long.parseLong(subject);
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
