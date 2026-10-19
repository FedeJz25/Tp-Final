package Excepciones;

/**
 * Se lanza cuando falla algo contra la base de datos (conexión, lectura o escritura).
 *
 * Hereda de OperacionTicketException a propósito: todos los
 * "catch (OperacionTicketException ex)" que ya existen en las ventanas
 * muestran este error al usuario sin tener que modificarlos.
 */
public class PersistenciaException extends OperacionTicketException {
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje);
        initCause(causa);
    }
}
