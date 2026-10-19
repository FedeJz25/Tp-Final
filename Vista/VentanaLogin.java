package Vista;

import Excepciones.AutenticacionException;
import Excepciones.ClaveInvalidaException;
import Excepciones.PersistenciaException;
import Modelo.*;

import javax.swing.*;
import java.awt.*;

public class VentanaLogin extends JFrame {
    private Sistema sistema;
    private JTextField txtId;
    private JPasswordField txtContrasena;

    public VentanaLogin(Sistema sistema) {
        this.sistema = sistema;
        setTitle("Sistema de Gestión de Tickets - Iniciar Sesión");
        setSize(420, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblTitulo = new JLabel("Control de Tickets", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCampos = new JPanel(new GridLayout(2, 2, 10, 15));
        panelCampos.add(new JLabel("ID de Usuario :"));
        txtId = new JTextField();
        panelCampos.add(txtId);

        panelCampos.add(new JLabel("Contraseña:"));
        txtContrasena = new JPasswordField();
        panelCampos.add(txtContrasena);

        panelPrincipal.add(panelCampos, BorderLayout.CENTER);

        JButton btnIngresar = new JButton("Iniciar Sesión");
        btnIngresar.addActionListener(e -> intentarLogin());
        getRootPane().setDefaultButton(btnIngresar);

        JLabel lblInfo = new JLabel("<html><center><small>Prueba inicial: Admin (ID: admin) | Trabajador (ID: 1001) | Técnico (ID: 2001)<br>Clave inicial = ID</small></center></html>", SwingConstants.CENTER);
        JPanel panelSur = new JPanel(new BorderLayout(5, 5));
        panelSur.add(btnIngresar, BorderLayout.NORTH);
        panelSur.add(lblInfo, BorderLayout.SOUTH);

        panelPrincipal.add(panelSur, BorderLayout.SOUTH);
        add(panelPrincipal);
    }

    private void intentarLogin() {
        String id = txtId.getText().trim();
        String pass = new String(txtContrasena.getPassword());

        try {
            Usuario usuario = sistema.login(id, pass);

            // Si la clave es igual al ID o fue blanqueada, está forzado a cambiarla antes de entrar
            if (usuario.isRequiereCambioClave()) {
                JOptionPane.showMessageDialog(this,
                        "Por seguridad, debe modificar su contraseña inicial antes de ingresar al sistema.",
                        "Cambio de Contraseña Obligatorio",
                        JOptionPane.WARNING_MESSAGE);

                boolean cambiada = mostrarDialogoCambioClave(this, sistema, usuario);
                if (!cambiada) {
                    return; // No entra si cancela el cambio obligatorio
                }
            }

            abrirVentanaPorRol(usuario);
            this.dispose();

        } catch (AutenticacionException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Acceso", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirVentanaPorRol(Usuario usuario) {
        if (usuario instanceof Administrador) {
            new VentanaAdministrador(sistema, (Administrador) usuario).setVisible(true);
        } else if (usuario instanceof Tecnico) {
            new VentanaTecnico(sistema, (Tecnico) usuario).setVisible(true);
        } else if (usuario instanceof Trabajador) {
            new VentanaTrabajador(sistema, (Trabajador) usuario).setVisible(true);
        }
    }

    // Método reutilizable para el cambio de contraseña obligatorio o voluntario.
    // Pide la clave dos veces (evita errores de tipeo) y guarda el cambio en la base a través de Sistema.
    public static boolean mostrarDialogoCambioClave(Component parent, Sistema sistema, Usuario usuario) {
        while (true) {
            JPasswordField pfNueva = new JPasswordField();
            JPasswordField pfRepetir = new JPasswordField();
            int opcion = JOptionPane.showConfirmDialog(
                    parent,
                    new Object[]{
                        "Ingrese la nueva contraseña (distinta a su ID, mínimo " + Usuario.LARGO_MINIMO_CLAVE + " caracteres):", pfNueva,
                        "Repita la nueva contraseña:", pfRepetir
                    },
                    "Cambiar Contraseña",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (opcion != JOptionPane.OK_OPTION) {
                return false;
            }

            String nuevaClave = new String(pfNueva.getPassword());
            String repetida = new String(pfRepetir.getPassword());
            if (!nuevaClave.equals(repetida)) {
                JOptionPane.showMessageDialog(parent, "Las contraseñas no coinciden.", "Contraseña Inválida", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                sistema.cambiarContrasena(usuario, nuevaClave);
                JOptionPane.showMessageDialog(parent, "Contraseña actualizada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (ClaveInvalidaException ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "Contraseña Inválida", JOptionPane.ERROR_MESSAGE);
            } catch (PersistenciaException ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }
}