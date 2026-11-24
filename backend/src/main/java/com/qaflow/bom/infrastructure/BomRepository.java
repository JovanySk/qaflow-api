package com.qaflow.bom.infrastructure;

import java.util.Optional;

import com.qaflow.bom.domain.Bom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BomRepository extends JpaRepository<Bom, Long> {

    @Query("select bom.id from Bom bom where bom.product.id = :productId and bom.current = true")
    Optional<Long> findCurrentId(@Param("productId") long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bom from Bom bom where bom.product.id = :productId and bom.current = true")
    Optional<Bom> findCurrentForUpdate(@Param("productId") long productId);

    @EntityGraph(attributePaths = {"product", "items", "items.material"})
    @Query("select bom from Bom bom where bom.product.id = :productId and bom.current = true")
    Optional<Bom> findCurrentDetail(@Param("productId") long productId);

    @EntityGraph(attributePaths = {"product", "items", "items.material"})
    @Query("select bom from Bom bom "
            + "where bom.product.id = :productId and bom.revisionNumber = :revisionNumber")
    Optional<Bom> findRevisionDetail(
            @Param("productId") long productId,
            @Param("revisionNumber") int revisionNumber);

    Page<Bom> findAllByProduct_Id(long productId, Pageable pageable);

    @Query("select coalesce(max(bom.revisionNumber), 0) from Bom bom where bom.product.id = :productId")
    int findMaxRevisionNumber(@Param("productId") long productId);
}
