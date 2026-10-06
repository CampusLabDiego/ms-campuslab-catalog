package cl.campuslab.catalog.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("Recurso no encontrado con id: " + id);
    }
}