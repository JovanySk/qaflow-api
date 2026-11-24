package com.qaflow.inventory.infrastructure;

import java.util.Optional;

import com.qaflow.inventory.domain.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long>, JpaSpecificationExecutor<InventoryMovement> {

    @EntityGraph(attributePaths = "material")
    Page<InventoryMovement> findAll(Specification<InventoryMovement> specification, Pageable pageable);

    @EntityGraph(attributePaths = "material")
    Optional<InventoryMovement> findDetailById(Long id);
}
