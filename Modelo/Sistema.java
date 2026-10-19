package Modelo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Excepciones.AutenticacionException;
import Excepciones.ClaveInvalidaException;
import Excepciones.LimiteTicketsException;
import Excepciones.OperacionTicketException;
import Excepciones.PersistenciaException;
import Persistencia.ConexionBD;
import Persistencia.TicketDAO;
import Persistencia.UsuarioDAO;

/**
 * Punto de entrada de toda la lógica del sistema (las ventanas solo hablan con esta clase).
 *
 * Cómo funciona la persistencia:
 *  1. Al arrancar, carga todo desde la base a las listas en memoria (usuarios y tickets).
 *  2. Cada operación que modifica algo: aplica la regla de negocio sobre los objetos
 *     y enseguida guarda los cambios en la base, TODOS juntos en una misma transacción.
 *  3. Las lecturas (login, listados) se hacen sobre la memoria, así que son rápidas.
 */
public class Sistema {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TicketDAO ticketDAO = new TicketDAO();

    public Sistema() throws PersistenciaException {
        ConexionBD.inicializar();
        cargarDesdeBD();
    }

    // =====================================================================
    //  Persistencia (infraestructura interna)
    // =====================================================================

    /** Una operación que se ejecuta dentro de una transacción. */
    @FunctionalInterface
    private interface OperacionBD {
        void ejecutar(Connection con) throws SQLException;
    }

    /**
     * Ejecuta varias escrituras como una unidad: o se guardan todas (commit) o no se guarda ninguna (rollback).
     * Evita estados a medias, por ejemplo "ticket actualizado pero la falla del técnico no".
     */
    private void enTransaccion(OperacionBD operacion) throws PersistenciaException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                operacion.ejecutar(con);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar en la base de datos: " + e.getMessage()
                    + "\nReinicie la aplicación para volver al último estado guardado.", e);
        }
    }

    private void cargarDesdeBD() throws PersistenciaException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            usuarios.addAll(usuarioDAO.listarTodos(con));

            Map<String, Usuario> usuariosPorId = new HashMap<>();
            for (Usuario u : usuarios) {
                usuariosPorId.put(u.getId(), u);
            }
            tickets.addAll(ticketDAO.listarTodos(con, usuariosPorId));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron cargar los datos: " + e.getMessage(), e);
        }
        reconstruirRelaciones();
    }

    /**
     * En la base cada ticket guarda solo el ID de su creador y de su técnico.
     * Acá se rearman las listas de los objetos: los tickets de cada trabajador
     * y los tickets que cada técnico tiene en atención (estados ATENDIDO o RESUELTO).
     */
    private void reconstruirRelaciones() {
        for (Ticket t : tickets) {
            t.getCreador().agregarTicket(t);
            Tecnico actual = t.getTecnicoActual();
            boolean enAtencion = t.getEstado() == EstadoTicket.ATENDIDO || t.getEstado() == EstadoTicket.RESUELTO;
            if (actual != null && enAtencion) {
                actual.restaurarTicketAtendido(t);
            }
        }
    }

    private void guardarUsuario(Usuario usuario) throws PersistenciaException {
        enTransaccion(con -> usuarioDAO.actualizar(con, usuario));
    }

    /** Guarda un ticket y, en la misma transacción, los usuarios que cambiaron junto con él (p. ej. las fallas del técnico). */
    private void guardarTicket(Ticket ticket, Usuario... usuariosAfectados) throws PersistenciaException {
        enTransaccion(con -> {
            ticketDAO.actualizar(con, ticket);
            for (Usuario u : usuariosAfectados) {
                if (u != null) {
                    usuarioDAO.actualizar(con, u);
                }
            }
        });
    }

    // =====================================================================
    //  Usuarios y sesión
    // =====================================================================

    public Usuario login(String id, String contrasena) throws AutenticacionException {
        if (id == null || id.trim().isEmpty()) {
            throw new AutenticacionException("Debe ingresar su ID de usuario.");
        }
        Usuario u = buscarUsuario(id.trim());
        if (u == null) {
            throw new AutenticacionException("ID o contraseña incorrectos.");
        }
        u.iniciarSesion(contrasena);
        return u;
    }

    /** Solo para el primer arranque: crea el administrador inicial cuando la base está vacía. */
    public void registrarAdministradorInicial(Administrador admin) throws OperacionTicketException {
        if (!usuarios.isEmpty()) {
            throw new OperacionTicketException("El administrador inicial solo puede crearse con la base de datos vacía.");
        }
        enTransaccion(con -> usuarioDAO.insertar(con, admin));
        usuarios.add(admin);
    }

    public void agregarUsuario(Usuario admin, Usuario nuevo) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        if (nuevo == null || nuevo.getId() == null || nuevo.getId().trim().isEmpty()) {
            throw new OperacionTicketException("El ID del nuevo usuario no puede estar vacío.");
        }
        if (buscarUsuario(nuevo.getId()) != null) {
            throw new OperacionTicketException("Ya existe un usuario registrado con el ID: " + nuevo.getId());
        }
        enTransaccion(con -> usuarioDAO.insertar(con, nuevo));
        this.usuarios.add(nuevo);
    }

    public void cambiarContrasena(Usuario usuario, String nuevaContrasena)
            throws ClaveInvalidaException, PersistenciaException {
        usuario.cambiarContrasena(nuevaContrasena);
        guardarUsuario(usuario);
    }

    public void bloquearUsuario(Usuario admin, String idUsuario) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        Usuario u = buscarUsuario(idUsuario);
        if (u == null) {
            throw new OperacionTicketException("Usuario no encontrado.");
        }
        if (u.equals(admin)) {
            throw new OperacionTicketException("No puede bloquear su propio usuario administrador.");
        }
        u.setBloqueado(true);
        guardarUsuario(u);
    }

    public void desbloquearUsuario(Usuario admin, String idUsuario) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        Usuario u = buscarUsuario(idUsuario);
        if (u == null) {
            throw new OperacionTicketException("Usuario no encontrado.");
        }
        u.setBloqueado(false);
        if (u instanceof Tecnico) {
            Tecnico t = (Tecnico) u;
            if (t.getFallas() >= 3) {
                t.reiniciarFallas();
            }
        }
        guardarUsuario(u);
    }

    public void blanquearContrasena(Usuario admin, String idUsuario) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        Usuario u = buscarUsuario(idUsuario);
        if (u == null) {
            throw new OperacionTicketException("Usuario no encontrado.");
        }
        u.blanquearClave();
        guardarUsuario(u);
    }

    public Usuario buscarUsuario(String id) {
        for (Usuario u : usuarios) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    public List<Tecnico> listarTecnicos(Usuario admin) {
        List<Tecnico> tecnicos = new ArrayList<>();
        if (admin instanceof Administrador) {
            for (Usuario u : usuarios) {
                if (u instanceof Tecnico) {
                    tecnicos.add((Tecnico) u);
                }
            }
        }
        return tecnicos;
    }

    // =====================================================================
    //  Tickets
    // =====================================================================

    public Ticket crearTicket(Trabajador creador, String titulo, String descripcion) throws OperacionTicketException {
        if (creador == null || creador.isBloqueado()) {
            throw new OperacionTicketException("El trabajador no puede crear tickets.");
        }
        if (titulo == null || titulo.trim().isEmpty() || descripcion == null || descripcion.trim().isEmpty()) {
            throw new OperacionTicketException("Debe completar tanto el título como la descripción detallada del ticket.");
        }
        Ticket nuevoTicket = new Ticket(titulo.trim(), descripcion.trim(), creador);
        enTransaccion(con -> ticketDAO.insertar(con, nuevoTicket)); // la base le asigna el ID
        this.tickets.add(nuevoTicket);
        creador.agregarTicket(nuevoTicket);
        return nuevoTicket;
    }

    public void tomarTicket(Tecnico tecnico, Ticket ticket) throws LimiteTicketsException, OperacionTicketException {
        tecnico.tomarTicket(ticket);
        guardarTicket(ticket);
    }

    public void resolverTicket(Tecnico tecnico, Ticket ticket, String notaResolucion) throws OperacionTicketException {
        tecnico.resolverTicket(ticket, notaResolucion);
        guardarTicket(ticket);
    }

    public void solicitarReapertura(Tecnico tecnico, Ticket ticket) throws OperacionTicketException {
        tecnico.solicitarReapertura(ticket);
        guardarTicket(ticket);
    }

    public void confirmarResolucion(Trabajador trabajador, Ticket ticket) throws OperacionTicketException {
        trabajador.confirmarResolucion(ticket);
        // el técnico puede haber descontado una falla, por eso también se guarda
        guardarTicket(ticket, ticket.getTecnicoActual());
    }

    public void rechazarResolucion(Trabajador trabajador, Ticket ticket, String motivoRechazo)
            throws OperacionTicketException {
        // Hay que guardar la referencia antes: rechazarResolucion libera al técnico del ticket.
        Tecnico tecnico = (ticket != null) ? ticket.getTecnicoActual() : null;
        trabajador.rechazarResolucion(ticket, motivoRechazo);
        guardarTicket(ticket, tecnico);
    }

    public void aprobarReaperturaTicket(Usuario admin, Ticket ticket) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        if (ticket == null || !ticket.isSolicitudReapertura()) {
            throw new OperacionTicketException("La solicitud de reapertura no existe.");
        }
        Tecnico tecnico = ticket.getTecnicoActual();
        if (tecnico != null) {
            tecnico.recibirMarcaRetorno();
        }
        ticket.cambiarEstado(EstadoTicket.REABIERTO); // también cierra la solicitud pendiente
        ticket.liberarTecnico();
        guardarTicket(ticket, tecnico);
    }

    public void reasignarTicketsCongelados(Usuario admin, Tecnico tecnicoBloqueado) throws OperacionTicketException {
        if (!(admin instanceof Administrador)) {
            throw new OperacionTicketException("No tiene permisos de Administrador.");
        }
        if (tecnicoBloqueado == null || !tecnicoBloqueado.isBloqueado()) {
            throw new OperacionTicketException("El técnico seleccionado no se encuentra bloqueado.");
        }
        List<Ticket> ticketsCongelados = new ArrayList<>(tecnicoBloqueado.getTicketsAtendidos());
        if (ticketsCongelados.isEmpty()) {
            throw new OperacionTicketException("El técnico no posee tickets activos para reasignar.");
        }
        for (Ticket ticket : ticketsCongelados) {
            ticket.cambiarEstado(EstadoTicket.REABIERTO);
            ticket.liberarTecnico();
        }
        enTransaccion(con -> {
            for (Ticket ticket : ticketsCongelados) {
                ticketDAO.actualizar(con, ticket);
            }
        });
    }

    public List<Ticket> listarTicketsPorEstado(EstadoTicket estado) {
        List<Ticket> filtrados = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getEstado() == estado) {
                filtrados.add(t);
            }
        }
        return filtrados;
    }

    public Ticket buscarTicketPorId(int idTicket) {
        for (Ticket t : tickets) {
            if (t.getIdticket() == idTicket) {
                return t;
            }
        }
        return null;
    }

    // =====================================================================
    //  Consultas
    // =====================================================================

    /** Tickets con solicitud de reapertura pendiente (ahora es un dato del propio ticket, así sobrevive al reinicio). */
    public List<Ticket> getSolicitudesReapertura() {
        List<Ticket> pendientes = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.isSolicitudReapertura()) {
                pendientes.add(t);
            }
        }
        return pendientes;
    }

    /** Listas de solo lectura: cualquier cambio tiene que pasar por Sistema para que también se guarde en la base. */
    public List<Usuario> getUsuarios() { return Collections.unmodifiableList(usuarios); }
    public List<Ticket> getTickets() { return Collections.unmodifiableList(tickets); }
}
