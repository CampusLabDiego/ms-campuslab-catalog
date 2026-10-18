package cl.campuslab.catalog.stock;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stock/bookings")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @PutMapping("/{bookingId}")
    public StockService.Receipt apply(
            @PathVariable Long bookingId,
            @Valid @RequestBody StockCommand command
    ) {
        return stock.apply(bookingId, command);
    }
}