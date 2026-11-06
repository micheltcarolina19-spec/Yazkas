package usta.veterinaria.domain.exception;

// Excepción que se lanza cuando no se cumple una regla de negocio del dominio
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
