package com.qaflow.user.infrastructure;

import java.util.List;
import java.util.Optional;

import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

    Optional<UserAccount> findByUsernameNormalized(String usernameNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user where user.id = :id")
    Optional<UserAccount> findByIdForUpdate(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user "
            + "where user.role = com.qaflow.user.domain.Role.ADMIN and user.active = true "
            + "order by user.id")
    List<UserAccount> lockActiveAdministrators();

    long countByRoleAndActiveTrue(Role role);
}
