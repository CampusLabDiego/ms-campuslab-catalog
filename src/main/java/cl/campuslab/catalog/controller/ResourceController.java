package cl.campuslab.catalog.controller;

import cl.campuslab.catalog.dto.ResourceRequest;
import cl.campuslab.catalog.dto.ResourceResponse;
import cl.campuslab.catalog.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> findAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request) {

        ResourceResponse resource = resourceService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {

        return ResponseEntity.ok(
                resourceService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        resourceService.delete(id);

        return ResponseEntity.noContent().build();
    }
}