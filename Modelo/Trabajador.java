package Modelo;

import java.util.List;
import java.util.ArrayList;
import Excepciones.OperacionTicketException;

public class Trabajador extends Usuario {
    private List<Ticket> misTickets;

    public Trabajador(String id) {
        super(id);
        this.misTickets = new ArrayList<>();
    }

    /** Reconstruye un trabajador guardado en la base de datos (sus tickets se vinculan después, al cargar). */
    public Trabajador(String id, String contrasenaHash, boolean bloqueado, boolean requiereCambioClave) {
        super(id, contrasenaHash, bloqueado, requiereCambioClave);
        this.misTickets = new ArrayList<>();
    }

    public void agregarTicket(Ticket ticket) {
        if (!this.misTickets.contains(ticket)) {
            this.misTickets.add(ticket);
        }
    }

    public List<Ticket> verTicketsPropios() {
        List<Ticket> activos = new ArrayList<>();
        for (Ticket t : misTickets) {
            if (t.getEstado() != EstadoTicket.FINALIZADO) {
                activos.add(t);
            }
        }
        return activos;
    }

    public void confirmarResolucion(Ticket ticket) throws OperacionTicketException {
        if (ticket == null || !this.misTickets.contains(ticket)) {
            throw new OperacionTicketException("El ticket seleccionado no pertenece a su lista.");
        }
        if (ticket.getEstado() != EstadoTicket.RESUELTO) {
            throw new OperacionTicketException("Solo se pueden confirmar tickets que estén en estado RESUELTO.");
        }

        ticket.cambiarEstado(EstadoTicket.FINALIZADO);

        Tecnico tecnico = ticket.getTecnicoActual();
        if (tecnico != null) {
            if (ticket.isFueReabierto()) {
                tecnico.descontarFalla();
            }
            tecnico.quitarTicketAtendido(ticket);
        }
    }

    public void rechazarResolucion(Ticket ticket, String motivoRechazo) throws OperacionTicketException {
        if (ticket == null || !this.misTickets.contains(ticket)) {
            throw new OperacionTicketException("El ticket seleccionado no pertenece a su lista.");
        }
        if (ticket.getEstado() != EstadoTicket.RESUELTO) {
            throw new OperacionTicketException("Solo se pueden rechazar tickets que estén en estado RESUELTO.");
        }
        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            throw new OperacionTicketException("Debe especificar el motivo del rechazo.");
        }

        ticket.setMotivoRechazo(motivoRechazo.trim());
        ticket.cambiarEstado(EstadoTicket.REABIERTO);

        Tecnico tecnico = ticket.getTecnicoActual();
        if (tecnico != null) {
            tecnico.recibirFalla();
        }
        ticket.liberarTecnico();
    }
}