package Vista;

import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import Excepciones.OperacionTicketException;
import Modelo.Administrador;
import Modelo.Sistema;
import Modelo.Tecnico;
import Modelo.Trabajador;

public class Main {
    public static void main(String[] args) {
        aplicarModoOscuro();
        SwingUtilities.invokeLater(() -> {
            Sistema sistema;
            try {
                // Abre (o crea) la base de datos y carga usuarios y tickets guardados
                sistema = new Sistema();
                if (sistema.getUsuarios().isEmpty()) {
                    cargarDatosIniciales(sistema); // solo la primera vez que se ejecuta
                }
            } catch (OperacionTicketException e) { // incluye PersistenciaException
                JOptionPane.showMessageDialog(null,
                        "No se pudo iniciar el sistema:\n" + e.getMessage(),
                        "Error de base de datos", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }

            new VentanaLogin(sistema).setVisible(true);
        });
    }

    /** Usuarios de prueba para el primer arranque (todos con clave inicial = ID y cambio obligatorio). */
    private static void cargarDatosIniciales(Sistema sistema) throws OperacionTicketException {
        Administrador admin = new Administrador("admin");
        sistema.registrarAdministradorInicial(admin);
        sistema.agregarUsuario(admin, new Trabajador("1001"));
        sistema.agregarUsuario(admin, new Tecnico("2001"));
    }

    private static void aplicarModoOscuro() {
        try {
            
            Color fondoPrincipal = new Color(36, 37, 42); 
            Color fondoInputsYTablas = new Color(24, 25, 28); 
            Color filaAlternaTabla = new Color(32, 33, 38);
            Color baseBotones = new Color(0, 112,112);
            Color bordes = new Color(59, 130, 246);     
            Color textoClaro = new Color(245, 245, 245); 
                                                         
            
            UIManager.put("control", fondoPrincipal);
            UIManager.put("info", fondoPrincipal);
            UIManager.put("nimbusBase", baseBotones);
            UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
            UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
            UIManager.put("nimbusFocus", bordes);
            UIManager.put("nimbusGreen", new Color(176, 179, 50));
            UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
            UIManager.put("nimbusLightBackground", fondoInputsYTablas);
            UIManager.put("nimbusOrange", new Color(191, 98, 4));
            UIManager.put("nimbusRed", new Color(169, 46, 34));
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("nimbusSelectionBackground", bordes);
            UIManager.put("text", textoClaro);

            // Ajustes específicos para bordes con título, tablas y campos de texto
            UIManager.put("TitledBorder.titleColor", textoClaro);
            UIManager.put("Table.background", fondoInputsYTablas);
            UIManager.put("Table.alternateRowColor", filaAlternaTabla);
            UIManager.put("Table.foreground", textoClaro);
            UIManager.put("Table.gridColor", new Color(55, 58, 66));
            UIManager.put("TextField.background", fondoInputsYTablas);
            UIManager.put("TextField.foreground", textoClaro);
            UIManager.put("TextField.caretForeground", Color.WHITE);
            UIManager.put("PasswordField.background", fondoInputsYTablas);
            UIManager.put("PasswordField.foreground", textoClaro);
            UIManager.put("PasswordField.caretForeground", Color.WHITE);
            UIManager.put("TextArea.background", fondoInputsYTablas);
            UIManager.put("TextArea.foreground", textoClaro);
            UIManager.put("TextArea.caretForeground", Color.WHITE);

            // Activamos el Look and Feel Nimbus de Java
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}