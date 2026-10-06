package cl.campuslab.catalog.dto;

import cl.campuslab.catalog.enums.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceRequest(

        @NotBlank
        String name,

        String description,

        @NotNull
        ResourceType type,

        @Min(0)
        Integer capacity,

        @Min(0)
        Integer stock,

        @Min(0)
        Integer availableStock,

        String location,

        Boolean active
) {
}