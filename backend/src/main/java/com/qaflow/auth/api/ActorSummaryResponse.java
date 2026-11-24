package com.qaflow.auth.api;

import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;

public record ActorSummaryResponse(
        String id,
        String username,
        String displayName,
        Role role) {

    public static ActorSummaryResponse from(QaFlowPrincipal principal) {
        return new ActorSummaryResponse(
                Long.toString(principal.id()),
                principal.username(),
                principal.displayName(),
                principal.role());
    }

    public static ActorSummaryResponse from(UserAccount user) {
        return new ActorSummaryResponse(
                Long.toString(user.id()),
                user.username(),
                user.displayName(),
                user.role());
    }
}
