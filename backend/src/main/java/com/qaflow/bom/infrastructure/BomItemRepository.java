package com.qaflow.bom.infrastructure;

import java.util.Collection;
import java.util.List;

import com.qaflow.bom.domain.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BomItemRepository extends JpaRepository<BomItem, Long> {

    @Query("select item.bom.id, count(item.id) from BomItem item "
            + "where item.bom.id in :bomIds group by item.bom.id")
    List<Object[]> countItemsByBomIds(@Param("bomIds") Collection<Long> bomIds);
}
