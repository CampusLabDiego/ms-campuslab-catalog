package cl.campuslab.catalog.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stock_allocations",
        indexes = @Index(
                name = "idx_stock_resource_state",
                columnList = "resource_id,state"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class StockAllocation {

    @Id
    private Long bookingId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockRules.State state;

    @Column(nullable = false)
    private boolean consumable;
}