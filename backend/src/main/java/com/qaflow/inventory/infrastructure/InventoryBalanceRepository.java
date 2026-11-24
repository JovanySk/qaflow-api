package com.qaflow.inventory.infrastructure;

import java.util.Optional;

import com.qaflow.inventory.domain.InventoryBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalance, Long>, JpaSpecificationExecutor<InventoryBalance> {

    @EntityGraph(attributePaths = "material")
    Page<InventoryBalance> findAll(Specification<InventoryBalance> specification, Pageable pageable);

    @EntityGraph(attributePaths = "material")
    Optional<InventoryBalance> findDetailByMaterialId(Long materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select balance from InventoryBalance balance join fetch balance.material "
            + "where balance.materialId = :materialId")
    Optional<InventoryBalance> findByMaterialIdForUpdate(@Param("materialId") long materialId);
}
