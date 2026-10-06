package cl.campuslab.catalog.dto;

import cl.campuslab.catalog.enums.ResourceType;

import java.time.LocalDateTime;

public record ResourceResponse(

        Long id,
        String name,
        String description,
        ResourceType type,
        Integer capacity,
        Integer stock,
        Integer availableStock,
        String location,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}