package cl.campuslab.catalog.stock;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAllocationRepository
        extends JpaRepository<StockAllocation, Long> {

    boolean existsByResourceId(Long resourceId);

    boolean existsByResourceIdAndState(
            Long resourceId,
            StockRules.State state
    );
}