package com.qaflow.security;

import java.util.List;

import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationConverter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Invalid token subject", exception);
        }
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid token user"));
        QaFlowPrincipal principal = new QaFlowPrincipal(
                user.id(), user.username(), user.displayName(), user.role(), user.authVersion());
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    }
}
