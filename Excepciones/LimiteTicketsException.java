package Excepciones;

public class LimiteTicketsException extends Exception {
    public LimiteTicketsException(String mensaje) {
        super(mensaje);
    }
}
