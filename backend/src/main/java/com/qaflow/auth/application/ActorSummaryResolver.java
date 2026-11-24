package com.qaflow.auth.application;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.springframework.stereotype.Component;

@Component
public class ActorSummaryResolver {

    private final UserAccountRepository userAccountRepository;

    public ActorSummaryResolver(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public Map<Long, ActorSummaryResponse> resolve(Collection<Long> actorIds) {
        HashSet<Long> uniqueIds = new HashSet<>(actorIds);
        Map<Long, ActorSummaryResponse> actors = userAccountRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        UserAccount::id,
                        ActorSummaryResponse::from,
                        (left, right) -> left));
        if (actors.size() != uniqueIds.size()) {
            throw new IllegalStateException("An audited catalog resource references an unknown user");
        }
        return actors;
    }
}
