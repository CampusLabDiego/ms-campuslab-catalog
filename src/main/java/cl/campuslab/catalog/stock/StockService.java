package cl.campuslab.catalog.stock;

import cl.campuslab.catalog.entity.Resource;
import cl.campuslab.catalog.enums.ResourceType;
import cl.campuslab.catalog.exception.ResourceNotFoundException;
import cl.campuslab.catalog.repository.ResourceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StockService {

    private final ResourceRepository resources;
    private final StockAllocationRepository allocations;

    public StockService(
            ResourceRepository resources,
            StockAllocationRepository allocations
    ) {
        this.resources = resources;
        this.allocations = allocations;
    }

    @Transactional
    public Receipt apply(
            Long bookingId,
            StockCommand command
    ) {

        if (bookingId == null || bookingId < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reserva inválida."
            );
        }

        // Bloquea el recurso mientras se procesa el movimiento.
        Resource resource = resources
                .findLockedById(command.resourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                command.resourceId()
                        )
                );

        StockAllocation allocation = allocations
                .findById(bookingId)
                .orElse(null);

        if (
                allocation != null
                && (
                        !allocation.getResourceId()
                                .equals(command.resourceId())
                        || allocation.getQuantity()
                                != command.quantity()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva ya tiene un movimiento "
                    + "con otro recurso o cantidad."
            );
        }

        StockRules.Result result;

        try {
            result = StockRules.apply(
                    resource.getStock(),
                    resource.getAvailableStock(),
                    command.quantity(),
                    command.action(),

                    allocation == null
                            ? null
                            : allocation.getState(),

                    allocation != null
                            && allocation.isConsumable(),

                    resource.getType() == ResourceType.SUPPLY,

                    Boolean.TRUE.equals(resource.getActive()),

                    resource.getType() == ResourceType.LABORATORY
            );
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    error.getMessage()
            );
        }

        if (allocation == null) {
            allocation = new StockAllocation();

            allocation.setBookingId(bookingId);
            allocation.setResourceId(resource.getId());
            allocation.setQuantity(command.quantity());
        }

        allocation.setState(result.state());
        allocation.setConsumable(result.consumable());

        resource.setStock(result.total());
        resource.setAvailableStock(result.available());

        resources.save(resource);
        allocations.saveAndFlush(allocation);

        return new Receipt(
                bookingId,
                resource.getId(),
                result.state(),
                result.total(),
                result.available()
        );
    }

    public record Receipt(
            Long bookingId,
            Long resourceId,
            StockRules.State state,
            int stock,
            int availableStock
    ) {
    }
}