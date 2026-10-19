package Modelo;

import java.time.LocalDateTime;

public class Ticket {
    private int idticket; // lo asigna la base de datos (AUTOINCREMENT) al guardar el ticket nuevo
    private String titulo;
    private String descripcion;
    private String notaResolucion;
    private String motivoRechazo;
    private boolean fueReabierto;
    private boolean solicitudReapertura; // el técnico pidió reabrirlo y el Administrador todavía no respondió
    private LocalDateTime fechaTicket;
    private EstadoTicket estado;
    private Trabajador creador;
    private Tecnico tecnicoActual;
    private Tecnico tecnicoAnterior;

    /** Ticket nuevo. Todavía no tiene ID: se lo asigna la base al insertarlo (ver TicketDAO.insertar). */
    public Ticket(String titulo, String descripcion, Trabajador creador) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.creador = creador;
        this.estado = EstadoTicket.NO_ATENDIDO;
        this.fueReabierto = false;
        this.solicitudReapertura = false;
        this.fechaTicket = LocalDateTime.now();
    }

    /** Reconstruye un ticket guardado en la base de datos, con todos sus campos tal como estaban. */
    public Ticket(int idticket, String titulo, String descripcion, String notaResolucion, String motivoRechazo,
                  boolean fueReabierto, boolean solicitudReapertura, LocalDateTime fechaTicket,
                  EstadoTicket estado, Trabajador creador, Tecnico tecnicoActual, Tecnico tecnicoAnterior) {
        this.idticket = idticket;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.notaResolucion = notaResolucion;
        this.motivoRechazo = motivoRechazo;
        this.fueReabierto = fueReabierto;
        this.solicitudReapertura = solicitudReapertura;
        this.fechaTicket = fechaTicket;
        this.estado = estado;
        this.creador = creador;
        this.tecnicoActual = tecnicoActual;
        this.tecnicoAnterior = tecnicoAnterior;
    }

    public void cambiarEstado(EstadoTicket nuevoEstado) {
        this.estado = nuevoEstado;
        if (nuevoEstado == EstadoTicket.REABIERTO) {
            this.fueReabierto = true;
        }
        // Una solicitud de reapertura solo tiene sentido mientras el ticket está ATENDIDO;
        // si cambia de estado por cualquier motivo, la solicitud pendiente deja de valer.
        if (nuevoEstado != EstadoTicket.ATENDIDO) {
            this.solicitudReapertura = false;
        }
    }

    public void asignarTecnico(Tecnico tecnico) {
        this.tecnicoActual = tecnico;
        this.cambiarEstado(EstadoTicket.ATENDIDO);
    }

    public void liberarTecnico() {
        if (this.tecnicoActual != null) {
            this.tecnicoAnterior = this.tecnicoActual;
            this.tecnicoActual.quitarTicketAtendido(this);
            this.tecnicoActual = null;
        }
    }

    public int getIdticket() { return idticket; }
    /** Solo lo debe usar la capa de persistencia, justo después de insertar el ticket en la base. */
    public void setIdticket(int idticket) { this.idticket = idticket; }
    public EstadoTicket getEstado() { return estado; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Trabajador getCreador() { return creador; }
    public Tecnico getTecnicoActual() { return tecnicoActual; }
    public Tecnico getTecnicoAnterior() { return tecnicoAnterior; }
    public boolean isFueReabierto() { return fueReabierto; }
    public boolean isSolicitudReapertura() { return solicitudReapertura; }
    public void setSolicitudReapertura(boolean solicitudReapertura) { this.solicitudReapertura = solicitudReapertura; }
    public String getNotaResolucion() { return notaResolucion; }
    public void setNotaResolucion(String nota) { this.notaResolucion = nota; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivo) { this.motivoRechazo = motivo; }
    public LocalDateTime getFechaTicket() { return fechaTicket; }
}