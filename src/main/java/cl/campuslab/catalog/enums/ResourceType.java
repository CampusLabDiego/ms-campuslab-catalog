package cl.campuslab.catalog.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceType {

    LABORATORY("LABORATORIO"),
    EQUIPMENT("EQUIPO"),
    SUPPLY("INSUMO");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}