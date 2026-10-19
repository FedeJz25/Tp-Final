package Modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import Excepciones.LimiteTicketsException;
import Excepciones.OperacionTicketException;

public class Tecnico extends Usuario {
    private int fallas;
    private int marcasRetorno;
    private List<Ticket> ticketsAtendidos;

    public Tecnico(String id) {
        super(id);
        this.fallas = 0;
        this.marcasRetorno = 0;
        this.ticketsAtendidos = new ArrayList<>();
    }

    /** Reconstruye un técnico guardado en la base de datos (sus tickets en atención se vinculan después, al cargar). */
    public Tecnico(String id, String contrasenaHash, boolean bloqueado, boolean requiereCambioClave,
                   int fallas, int marcasRetorno) {
        super(id, contrasenaHash, bloqueado, requiereCambioClave);
        this.fallas = fallas;
        this.marcasRetorno = marcasRetorno;
        this.ticketsAtendidos = new ArrayList<>();
    }

    public List<Ticket> verTicketsPendientes(Sistema sistema) {
        List<Ticket> pendientes = new ArrayList<>();
        pendientes.addAll(sistema.listarTicketsPorEstado(EstadoTicket.NO_ATENDIDO));
        pendientes.addAll(sistema.listarTicketsPorEstado(EstadoTicket.REABIERTO));
        return pendientes;
    }

    public void tomarTicket(Ticket ticket) throws LimiteTicketsException, OperacionTicketException {
        if (this.isBloqueado()) {
            throw new OperacionTicketException("Su usuario se encuentra bloqueado.");
        }
        if (this.ticketsAtendidos.size() >= 3) {
            throw new LimiteTicketsException("No puede tomar el ticket: ya alcanzó el límite máximo de 3 tickets en atención simultánea.");
        }
        if (ticket == null || (ticket.getEstado() != EstadoTicket.NO_ATENDIDO && ticket.getEstado() != EstadoTicket.REABIERTO)) {
            throw new OperacionTicketException("El ticket seleccionado no se encuentra pendiente de atención.");
        }

        this.ticketsAtendidos.add(ticket);
        ticket.asignarTecnico(this);
    }

    public void resolverTicket(Ticket ticket, String notaResolucion) throws OperacionTicketException {
        if (this.isBloqueado()) {
            throw new OperacionTicketException("Su usuario se encuentra bloqueado.");
        }
        if (ticket == null || !this.ticketsAtendidos.contains(ticket)) {
            throw new OperacionTicketException("El ticket no se encuentra en su lista de atención.");
        }
        if (ticket.getEstado() != EstadoTicket.ATENDIDO) {
            throw new OperacionTicketException("El ticket ya fue marcado como resuelto anteriormente.");
        }
        if (ticket.isSolicitudReapertura()) {
            throw new OperacionTicketException("El ticket tiene una solicitud de reapertura pendiente. Espere la respuesta del Administrador.");
        }
        if (notaResolucion == null || notaResolucion.trim().isEmpty()) {
            throw new OperacionTicketException("Debe ingresar una nota o explicación de la solución aplicada.");
        }

        ticket.setNotaResolucion(notaResolucion.trim());
        ticket.cambiarEstado(EstadoTicket.RESUELTO);
    }

    public void solicitarReapertura(Ticket ticket) throws OperacionTicketException {
        if (this.isBloqueado()) {
            throw new OperacionTicketException("Su usuario se encuentra bloqueado.");
        }
        if (ticket == null || !this.ticketsAtendidos.contains(ticket)) {
            throw new OperacionTicketException("El ticket no pertenece a su lista de atención.");
        }
        if (ticket.getEstado() != EstadoTicket.ATENDIDO) {
            throw new OperacionTicketException("Solo puede solicitar la reapertura de un ticket en estado ATENDIDO.");
        }
        if (ticket.isSolicitudReapertura()) {
            throw new OperacionTicketException("Ya existe una solicitud de reapertura pendiente para este ticket.");
        }
        ticket.setSolicitudReapertura(true);
    }

    public void recibirMarcaRetorno() {
        if (this.marcasRetorno > 0) {
            this.marcasRetorno = 0;
            this.recibirFalla();
        } else {
            this.marcasRetorno++;
        }
    }

    public void recibirFalla() {
        this.fallas++;
        if (this.fallas >= 3) {
            this.setBloqueado(true);
        }
    }

    public void descontarFalla() {
        if (this.fallas > 0) {
            this.fallas--;
        }
    }

    public void reiniciarFallas() {
        this.fallas = 0;
    }

    public void quitarTicketAtendido(Ticket ticket) {
        this.ticketsAtendidos.remove(ticket);
    }

    /** Solo para la carga desde la base de datos: vuelve a vincular un ticket que este técnico tenía en atención. */
    public void restaurarTicketAtendido(Ticket ticket) {
        if (!this.ticketsAtendidos.contains(ticket)) {
            this.ticketsAtendidos.add(ticket);
        }
    }

    /** Vista de solo lectura: la lista solo se modifica a través de tomarTicket / quitarTicketAtendido. */
    public List<Ticket> getTicketsAtendidos() { return Collections.unmodifiableList(ticketsAtendidos); }
    public int getFallas() { return fallas; }
    public int getMarcasRetorno() { return marcasRetorno; }
}