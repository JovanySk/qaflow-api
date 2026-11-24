package com.qaflow.security;

import java.util.Collection;
import java.util.List;

import com.qaflow.user.domain.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class UserAccountDetails implements UserDetails {

    private final UserAccount userAccount;

    public UserAccountDetails(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public UserAccount userAccount() {
        return userAccount;
    }

    public QaFlowPrincipal principal() {
        return new QaFlowPrincipal(
                userAccount.id(),
                userAccount.username(),
                userAccount.displayName(),
                userAccount.role(),
                userAccount.authVersion());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userAccount.role().name()));
    }

    @Override
    public String getPassword() {
        return userAccount.passwordHash();
    }

    @Override
    public String getUsername() {
        return userAccount.username();
    }

    @Override
    public boolean isEnabled() {
        return userAccount.active();
    }
}
