package Vista;

import Excepciones.OperacionTicketException;
import Modelo.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VentanaTrabajador extends JFrame {
    private Sistema sistema;
    private Trabajador trabajador;
    private JTextField txtTitulo;
    private JTextArea txtDescripcion;
    private JTable tablaTickets;
    private DefaultTableModel modeloTabla;

    public VentanaTrabajador(Sistema sistema, Trabajador trabajador) {
        this.sistema = sistema;
        this.trabajador = trabajador;

        setTitle("Panel del Trabajador - ID: " + trabajador.getId());
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        // Barra superior
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panelSuperior.add(new JLabel("Trabajador conectado: " + trabajador.getId()), BorderLayout.WEST);

        JPanel panelBotonesSup = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCambiarClave = new JButton("Cambiar Contraseña");
        btnCambiarClave.addActionListener(e -> VentanaLogin.mostrarDialogoCambioClave(this, sistema, trabajador));
        JButton btnSalir = new JButton("Cerrar Sesión");
        btnSalir.addActionListener(e -> {
            new VentanaLogin(sistema).setVisible(true);
            dispose();
        });
        panelBotonesSup.add(btnCambiarClave);
        panelBotonesSup.add(btnSalir);
        panelSuperior.add(panelBotonesSup, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // Panel central dividido: Crear Ticket arriba, Tabla de tickets activos abajo
        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Formulario de nuevo ticket
        JPanel panelCrear = new JPanel(new BorderLayout(5, 5));
        panelCrear.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Ticket"));

        JPanel panelTitulo = new JPanel(new BorderLayout(5, 5));
        panelTitulo.add(new JLabel("Título breve:"), BorderLayout.WEST);
        txtTitulo = new JTextField();
        panelTitulo.add(txtTitulo, BorderLayout.CENTER);

        txtDescripcion = new JTextArea(3, 30);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);

        JButton btnCrear = new JButton("Emitir Ticket");
        btnCrear.addActionListener(e -> crearNuevoTicket());

        panelCrear.add(panelTitulo, BorderLayout.NORTH);
        panelCrear.add(scrollDesc, BorderLayout.CENTER);
        panelCrear.add(btnCrear, BorderLayout.SOUTH);

        panelCentro.add(panelCrear, BorderLayout.NORTH);

        // Tabla de tickets propios no finalizados
        JPanel panelLista = new JPanel(new BorderLayout(5, 5));
        panelLista.setBorder(BorderFactory.createTitledBorder("Mis Tickets Activos (No Finalizados)"));

        String[] columnas = {"ID", "Título", "Estado", "Técnico Asignado", "Nota de Resolución"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaTickets = new JTable(modeloTabla);
        panelLista.add(new JScrollPane(tablaTickets), BorderLayout.CENTER);

        // Botones de acción sobre el ticket seleccionado
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnConfirmar = new JButton("Confirmar Resolución (Finalizar)");
        btnConfirmar.addActionListener(e -> confirmarResolucion());

        JButton btnRechazar = new JButton("Rechazar Resolución (Reabrir)");
        btnRechazar.addActionListener(e -> rechazarResolucion());

        panelAcciones.add(btnConfirmar);
        panelAcciones.add(btnRechazar);
        panelLista.add(panelAcciones, BorderLayout.SOUTH);

        panelCentro.add(panelLista, BorderLayout.CENTER);
        add(panelCentro, BorderLayout.CENTER);

        actualizarTabla();
    }

    private void crearNuevoTicket() {
        try {
            sistema.crearTicket(trabajador, txtTitulo.getText(), txtDescripcion.getText());
            txtTitulo.setText("");
            txtDescripcion.setText("");
            actualizarTabla();
            JOptionPane.showMessageDialog(this, "Ticket creado exitosamente en estado NO_ATENDIDO.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al crear ticket", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Ticket obtenerTicketSeleccionado() {
        int fila = tablaTickets.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un ticket de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int idTicket = (int) modeloTabla.getValueAt(fila, 0);
        return sistema.buscarTicketPorId(idTicket);
    }

    private void confirmarResolucion() {
        Ticket ticket = obtenerTicketSeleccionado();
        if (ticket == null) return;

        try {
            sistema.confirmarResolucion(trabajador, ticket);
            actualizarTabla();
            JOptionPane.showMessageDialog(this, "Ticket confirmado y pasado a estado FINALIZADO.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no válida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechazarResolucion() {
        Ticket ticket = obtenerTicketSeleccionado();
        if (ticket == null) return;

        String motivo = JOptionPane.showInputDialog(this, "Especifique el motivo por el cual rechaza la solución:");
        if (motivo == null) return; // Canceló el cuadro de diálogo

        try {
            sistema.rechazarResolucion(trabajador, ticket, motivo);
            actualizarTabla();
            JOptionPane.showMessageDialog(this, "Resolución rechazada. El ticket pasó a estado REABIERTO y se computó la falla al técnico.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no válida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTabla() {
        modeloTabla.setRowCount(0);
        List<Ticket> activos = trabajador.verTicketsPropios();
        for (Ticket t : activos) {
            String tec = (t.getTecnicoActual() != null) ? t.getTecnicoActual().getId() : "Sin asignar";
            String nota = (t.getNotaResolucion() != null) ? t.getNotaResolucion() : "-";
            modeloTabla.addRow(new Object[]{
                    t.getIdticket(),
                    t.getTitulo(),
                    t.getEstado(),
                    tec,
                    nota
            });
        }
    }
}