package Persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import Excepciones.PersistenciaException;

/**
 * Único lugar que sabe "a qué base de datos nos conectamos" y cómo es su estructura.
 * Si mañana se cambia SQLite por MySQL, solo se toca esta clase (URL, driver y
 * alguna sentencia CREATE TABLE) y, como mucho, detalles menores de los DAO.
 */
public final class ConexionBD {

    /** Archivo de base de datos. Se crea solo la primera vez, en la carpeta desde donde se ejecuta el programa. */
    private static final String URL = "jdbc:sqlite:tickets.db";

    private static final String CREAR_USUARIOS =
            "CREATE TABLE IF NOT EXISTS usuarios ("
            + " id                    TEXT    PRIMARY KEY,"
            + " rol                   TEXT    NOT NULL CHECK (rol IN ('ADMINISTRADOR','TECNICO','TRABAJADOR')),"
            + " contrasena_hash       TEXT    NOT NULL,"
            + " bloqueado             INTEGER NOT NULL DEFAULT 0 CHECK (bloqueado IN (0,1)),"
            + " requiere_cambio_clave INTEGER NOT NULL DEFAULT 1 CHECK (requiere_cambio_clave IN (0,1)),"
            + " fallas                INTEGER NOT NULL DEFAULT 0 CHECK (fallas >= 0),"
            + " marcas_retorno        INTEGER NOT NULL DEFAULT 0 CHECK (marcas_retorno >= 0)"
            + ")";

    private static final String CREAR_TICKETS =
            "CREATE TABLE IF NOT EXISTS tickets ("
            + " id                   INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " titulo               TEXT    NOT NULL,"
            + " descripcion          TEXT    NOT NULL,"
            + " estado               TEXT    NOT NULL CHECK (estado IN ('NO_ATENDIDO','ATENDIDO','RESUELTO','FINALIZADO','REABIERTO')),"
            + " fecha_creacion       TEXT    NOT NULL,"
            + " nota_resolucion      TEXT,"
            + " motivo_rechazo       TEXT,"
            + " fue_reabierto        INTEGER NOT NULL DEFAULT 0 CHECK (fue_reabierto IN (0,1)),"
            + " solicitud_reapertura INTEGER NOT NULL DEFAULT 0 CHECK (solicitud_reapertura IN (0,1)),"
            + " id_creador           TEXT    NOT NULL REFERENCES usuarios(id),"
            + " id_tecnico_actual    TEXT    REFERENCES usuarios(id),"
            + " id_tecnico_anterior  TEXT    REFERENCES usuarios(id)"
            + ")";

    private static final String[] INDICES = {
            "CREATE INDEX IF NOT EXISTS idx_tickets_estado  ON tickets(estado)",
            "CREATE INDEX IF NOT EXISTS idx_tickets_creador ON tickets(id_creador)",
            "CREATE INDEX IF NOT EXISTS idx_tickets_tecnico ON tickets(id_tecnico_actual)"
    };

    private ConexionBD() {
    }

    /**
     * Abre una conexión nueva. Quien la pida debe cerrarla (try-with-resources).
     * En SQLite las claves foráneas vienen DESACTIVADAS por defecto, por eso se activan en cada conexión.
     */
    public static Connection obtenerConexion() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }

    /** Crea las tablas si todavía no existen. Se llama una vez al arrancar el sistema. */
    public static void inicializar() throws PersistenciaException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new PersistenciaException(
                    "No se encontró el driver de SQLite. Agregue el JAR 'sqlite-jdbc' a las librerías del proyecto.", e);
        }
        try (Connection con = obtenerConexion(); Statement st = con.createStatement()) {
            st.executeUpdate(CREAR_USUARIOS);
            st.executeUpdate(CREAR_TICKETS);
            for (String indice : INDICES) {
                st.executeUpdate(indice);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo abrir o crear la base de datos: " + e.getMessage(), e);
        }
    }
}