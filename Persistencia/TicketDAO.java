package Persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Modelo.EstadoTicket;
import Modelo.Tecnico;
import Modelo.Ticket;
import Modelo.Trabajador;
import Modelo.Usuario;

/**
 * DAO de tickets: la única clase que escribe SQL sobre la tabla "tickets".
 * En la base, las referencias a otros objetos (creador, técnico) se guardan como el ID del usuario.
 */
public class TicketDAO {

    /** Inserta el ticket y le asigna el ID que generó la base (reemplaza al viejo contador estático). */
    public void insertar(Connection con, Ticket t) throws SQLException {
        String sql = "INSERT INTO tickets (titulo, descripcion, estado, fecha_creacion, nota_resolucion, motivo_rechazo,"
                + " fue_reabierto, solicitud_reapertura, id_creador, id_tecnico_actual, id_tecnico_anterior)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getEstado().name());
            ps.setString(4, t.getFechaTicket().toString());
            setTexto(ps, 5, t.getNotaResolucion());
            setTexto(ps, 6, t.getMotivoRechazo());
            ps.setInt(7, t.isFueReabierto() ? 1 : 0);
            ps.setInt(8, t.isSolicitudReapertura() ? 1 : 0);
            ps.setString(9, t.getCreador().getId());
            setTexto(ps, 10, idDe(t.getTecnicoActual()));
            setTexto(ps, 11, idDe(t.getTecnicoAnterior()));
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("La base de datos no devolvió el ID del ticket creado.");
                }
                t.setIdticket(claves.getInt(1));
            }
        }
    }

    /** Guarda los campos de un ticket que pueden cambiar (título, descripción, creador y fecha no cambian nunca). */
    public void actualizar(Connection con, Ticket t) throws SQLException {
        String sql = "UPDATE tickets SET estado = ?, nota_resolucion = ?, motivo_rechazo = ?, fue_reabierto = ?,"
                + " solicitud_reapertura = ?, id_tecnico_actual = ?, id_tecnico_anterior = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getEstado().name());
            setTexto(ps, 2, t.getNotaResolucion());
            setTexto(ps, 3, t.getMotivoRechazo());
            ps.setInt(4, t.isFueReabierto() ? 1 : 0);
            ps.setInt(5, t.isSolicitudReapertura() ? 1 : 0);
            setTexto(ps, 6, idDe(t.getTecnicoActual()));
            setTexto(ps, 7, idDe(t.getTecnicoAnterior()));
            ps.setInt(8, t.getIdticket());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No existe en la base el ticket #" + t.getIdticket());
            }
        }
    }

    /**
     * Devuelve todos los tickets ya "armados" con sus objetos Trabajador y Tecnico reales.
     * Por eso hay que cargar los usuarios primero y pasarlos en el mapa.
     */
    public List<Ticket> listarTodos(Connection con, Map<String, Usuario> usuariosPorId) throws SQLException {
        String sql = "SELECT id, titulo, descripcion, estado, fecha_creacion, nota_resolucion, motivo_rechazo,"
                + " fue_reabierto, solicitud_reapertura, id_creador, id_tecnico_actual, id_tecnico_anterior"
                + " FROM tickets ORDER BY id";
        List<Ticket> lista = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Trabajador creador = buscar(usuariosPorId, rs.getString("id_creador"), Trabajador.class, id);
                Tecnico actual = buscar(usuariosPorId, rs.getString("id_tecnico_actual"), Tecnico.class, id);
                Tecnico anterior = buscar(usuariosPorId, rs.getString("id_tecnico_anterior"), Tecnico.class, id);

                lista.add(new Ticket(
                        id,
                        rs.getString("titulo"),
                        rs.getString("descripcion"),
                        rs.getString("nota_resolucion"),
                        rs.getString("motivo_rechazo"),
                        rs.getInt("fue_reabierto") == 1,
                        rs.getInt("solicitud_reapertura") == 1,
                        LocalDateTime.parse(rs.getString("fecha_creacion")),
                        EstadoTicket.valueOf(rs.getString("estado")),
                        creador,
                        actual,
                        anterior));
            }
        }
        return lista;
    }

    // ---- utilidades internas ----

    private static String idDe(Usuario u) {
        return (u == null) ? null : u.getId();
    }

    private static void setTexto(PreparedStatement ps, int posicion, String valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicion, Types.VARCHAR);
        } else {
            ps.setString(posicion, valor);
        }
    }

    /** Busca un usuario por ID y verifica que tenga el rol esperado; si no, avisa que la base está inconsistente. */
    private static <T extends Usuario> T buscar(Map<String, Usuario> mapa, String id, Class<T> tipo, int idTicket)
            throws SQLException {
        if (id == null) {
            return null;
        }
        Usuario u = mapa.get(id);
        if (!tipo.isInstance(u)) {
            throw new SQLException("Ticket #" + idTicket + ": el usuario '" + id
                    + "' no existe o no es de tipo " + tipo.getSimpleName());
        }
        return tipo.cast(u);
    }
}