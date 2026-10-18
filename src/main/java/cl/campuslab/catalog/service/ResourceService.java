package cl.campuslab.catalog.service;

import cl.campuslab.catalog.dto.ResourceRequest;
import cl.campuslab.catalog.dto.ResourceResponse;
import cl.campuslab.catalog.entity.Resource;
import cl.campuslab.catalog.exception.ResourceNotFoundException;
import cl.campuslab.catalog.repository.ResourceRepository;
import cl.campuslab.catalog.stock.StockAllocationRepository;
import cl.campuslab.catalog.stock.StockRules;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ResourceService {

    private final ResourceRepository resources;
    private final StockAllocationRepository allocations;

    public ResourceService(
            ResourceRepository resources,
            StockAllocationRepository allocations
    ) {
        this.resources = resources;
        this.allocations = allocations;
    }

    public List<ResourceResponse> findAll() {
        return resources.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ResourceResponse findById(Long id) {
        Resource resource = resources.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(id)
                );

        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        validate(request);

        Resource resource = new Resource();

        copy(request, resource);

        return toResponse(resources.save(resource));
    }

    @Transactional
    public ResourceResponse update(
            Long id,
            ResourceRequest request
    ) {
        validate(request);

        Resource resource = locked(id);

        boolean tieneUnidadesComprometidas =
                allocations.existsByResourceIdAndState(
                        id,
                        StockRules.State.RESERVED
                );

        boolean cambiaInventarioOTipo =
                !Objects.equals(
                        resource.getStock(),
                        request.stock()
                )
                || !Objects.equals(
                        resource.getAvailableStock(),
                        request.availableStock()
                )
                || resource.getType() != request.type();

        if (
                tieneUnidadesComprometidas
                && cambiaInventarioOTipo
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puedes modificar stocks o tipo "
                    + "mientras existan unidades comprometidas."
            );
        }

        copy(request, resource);

        return toResponse(resources.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = locked(id);

        if (allocations.existsByResourceId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recurso tiene historial de stock. "
                    + "Desactívalo para conservar la trazabilidad."
            );
        }

        resources.delete(resource);
    }

    private Resource locked(Long id) {
        return resources.findLockedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(id)
                );
    }

    private void validate(ResourceRequest request) {
        if (
                request.stock() == null
                || request.availableStock() == null
                || request.stock() < 0
                || request.availableStock() < 0
                || request.availableStock() > request.stock()
                || (
                        request.capacity() != null
                        && request.capacity() < 0
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El stock debe ser no negativo y disponible "
                    + "no puede superar al total."
            );
        }
    }

    private void copy(
            ResourceRequest request,
            Resource resource
    ) {
        resource.setName(request.name().trim());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setStock(request.stock());
        resource.setAvailableStock(request.availableStock());
        resource.setLocation(request.location());

        if (request.active() != null) {
            resource.setActive(request.active());
        } else if (resource.getActive() == null) {
            resource.setActive(true);
        }
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getCapacity(),
                resource.getStock(),
                resource.getAvailableStock(),
                resource.getLocation(),
                resource.getActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}