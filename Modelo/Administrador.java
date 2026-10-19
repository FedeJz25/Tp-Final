package Modelo;

import java.util.List;
import Excepciones.OperacionTicketException;

public class Administrador extends Usuario {

    public Administrador(String id) {
        super(id);
    }

    /** Reconstruye un administrador guardado en la base de datos. */
    public Administrador(String id, String contrasenaHash, boolean bloqueado, boolean requiereCambioClave) {
        super(id, contrasenaHash, bloqueado, requiereCambioClave);
    }

    public void reasignarTicketsCongelados(Sistema sistema, Tecnico tecnicoBloqueado) throws OperacionTicketException {
        sistema.reasignarTicketsCongelados(this, tecnicoBloqueado);
    }

    public List<Tecnico> verMetricasTecnicos(Sistema sistema) {
        return sistema.listarTecnicos(this);
    }

    public List<Ticket> auditarTodosLosTickets(Sistema sistema) {
        return sistema.getTickets();
    }
}