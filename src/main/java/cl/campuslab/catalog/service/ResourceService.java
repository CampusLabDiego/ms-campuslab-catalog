package cl.campuslab.catalog.service;

import cl.campuslab.catalog.dto.ResourceRequest;
import cl.campuslab.catalog.dto.ResourceResponse;
import cl.campuslab.catalog.entity.Resource;
import cl.campuslab.catalog.exception.ResourceNotFoundException;
import cl.campuslab.catalog.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ResourceResponse findById(Long id) {
        Resource resource = findEntityById(id);
        return toResponse(resource);
    }

    public ResourceResponse create(ResourceRequest request) {

        Resource resource = new Resource();

        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setStock(request.stock());
        resource.setAvailableStock(request.availableStock());
        resource.setLocation(request.location());

        if (request.active() == null) {
            resource.setActive(true);
        } else {
            resource.setActive(request.active());
        }

        Resource savedResource = resourceRepository.save(resource);

        return toResponse(savedResource);
    }

    public ResourceResponse update(Long id, ResourceRequest request) {

        Resource resource = findEntityById(id);

        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setStock(request.stock());
        resource.setAvailableStock(request.availableStock());
        resource.setLocation(request.location());

        if (request.active() != null) {
            resource.setActive(request.active());
        }

        Resource updatedResource = resourceRepository.save(resource);

        return toResponse(updatedResource);
    }

    public void delete(Long id) {

        Resource resource = findEntityById(id);

        resourceRepository.delete(resource);
    }

    private Resource findEntityById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
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