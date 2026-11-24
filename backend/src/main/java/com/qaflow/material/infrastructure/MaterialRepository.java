package com.qaflow.material.infrastructure;

import java.util.Collection;
import java.util.List;

import com.qaflow.material.domain.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    boolean existsByCodeNormalized(String codeNormalized);

    boolean existsByCodeNormalizedAndIdNot(String codeNormalized, long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select material from Material material "
            + "where material.id in :ids order by material.id")
    List<Material> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
