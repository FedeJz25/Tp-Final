package Persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import Modelo.Administrador;
import Modelo.Tecnico;
import Modelo.Trabajador;
import Modelo.Usuario;

/**
 * DAO (Data Access Object) de usuarios: la única clase que escribe SQL sobre la tabla "usuarios".
 * Las clases del modelo no saben nada de SQL; este DAO traduce objeto <-> fila.
 *
 * Los métodos reciben la Connection desde afuera para que quien los llama (Sistema)
 * pueda agrupar varios cambios en una sola transacción.
 */
public class UsuarioDAO {

    public void insertar(Connection con, Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios (id, rol, contrasena_hash, bloqueado, requiere_cambio_clave, fallas, marcas_retorno)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getId());
            ps.setString(2, rolDe(u));
            ps.setString(3, u.getContrasenaHash());
            ps.setInt(4, u.isBloqueado() ? 1 : 0);
            ps.setInt(5, u.isRequiereCambioClave() ? 1 : 0);
            ps.setInt(6, (u instanceof Tecnico) ? ((Tecnico) u).getFallas() : 0);
            ps.setInt(7, (u instanceof Tecnico) ? ((Tecnico) u).getMarcasRetorno() : 0);
            ps.executeUpdate();
        }
    }

    public void actualizar(Connection con, Usuario u) throws SQLException {
        String sql = "UPDATE usuarios SET contrasena_hash = ?, bloqueado = ?, requiere_cambio_clave = ?,"
                + " fallas = ?, marcas_retorno = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getContrasenaHash());
            ps.setInt(2, u.isBloqueado() ? 1 : 0);
            ps.setInt(3, u.isRequiereCambioClave() ? 1 : 0);
            ps.setInt(4, (u instanceof Tecnico) ? ((Tecnico) u).getFallas() : 0);
            ps.setInt(5, (u instanceof Tecnico) ? ((Tecnico) u).getMarcasRetorno() : 0);
            ps.setString(6, u.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No existe en la base el usuario con ID: " + u.getId());
            }
        }
    }

    /** Devuelve todos los usuarios, en el orden en que fueron creados. */
    public List<Usuario> listarTodos(Connection con) throws SQLException {
        String sql = "SELECT id, rol, contrasena_hash, bloqueado, requiere_cambio_clave, fallas, marcas_retorno"
                + " FROM usuarios ORDER BY rowid";
        List<Usuario> lista = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String hash = rs.getString("contrasena_hash");
                boolean bloqueado = rs.getInt("bloqueado") == 1;
                boolean requiereCambio = rs.getInt("requiere_cambio_clave") == 1;
                String rol = rs.getString("rol");

                switch (rol) {
                    case "ADMINISTRADOR":
                        lista.add(new Administrador(id, hash, bloqueado, requiereCambio));
                        break;
                    case "TECNICO":
                        lista.add(new Tecnico(id, hash, bloqueado, requiereCambio,
                                rs.getInt("fallas"), rs.getInt("marcas_retorno")));
                        break;
                    case "TRABAJADOR":
                        lista.add(new Trabajador(id, hash, bloqueado, requiereCambio));
                        break;
                    default:
                        throw new SQLException("Rol desconocido en la base de datos: " + rol);
                }
            }
        }
        return lista;
    }

    private static String rolDe(Usuario u) throws SQLException {
        if (u instanceof Administrador) return "ADMINISTRADOR";
        if (u instanceof Tecnico) return "TECNICO";
        if (u instanceof Trabajador) return "TRABAJADOR";
        throw new SQLException("Tipo de usuario no soportado: " + u.getClass().getName());
    }
}