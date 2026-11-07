package usta.veterinaria.application.exception;

// Excepción que se lanza cuando un recurso no existe
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
