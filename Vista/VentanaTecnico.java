package Vista;

import Excepciones.LimiteTicketsException;
import Excepciones.OperacionTicketException;
import Modelo.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VentanaTecnico extends JFrame {
    private Sistema sistema;
    private Tecnico tecnico;
    private JLabel lblMetricas;
    private JTable tablaPendientes;
    private DefaultTableModel modeloPendientes;
    private JTable tablaMisTickets;
    private DefaultTableModel modeloMisTickets;

    public VentanaTecnico(Sistema sistema, Tecnico tecnico) {
        this.sistema = sistema;
        this.tecnico = tecnico;

        setTitle("Panel del Técnico - ID: " + tecnico.getId());
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Barra superior
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        lblMetricas = new JLabel();
        lblMetricas.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelSuperior.add(lblMetricas, BorderLayout.WEST);

        JPanel panelBotonesSup = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCambiarClave = new JButton("Cambiar Contraseña");
        btnCambiarClave.addActionListener(e -> VentanaLogin.mostrarDialogoCambioClave(this, sistema, tecnico));
        JButton btnSalir = new JButton("Cerrar Sesión");
        btnSalir.addActionListener(e -> {
            new VentanaLogin(sistema).setVisible(true);
            dispose();
        });
        panelBotonesSup.add(btnCambiarClave);
        panelBotonesSup.add(btnSalir);
        panelSuperior.add(panelBotonesSup, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // Pestañas para separar la bolsa de pendientes y los tickets que está atendiendo
        JTabbedPane pestanas = new JTabbedPane();

        // Pestaña 1: Bolsa de tickets pendientes (NO_ATENDIDO y REABIERTO)
        JPanel panelPendientes = new JPanel(new BorderLayout(5, 5));
        String[] colPendientes = {"ID", "Título", "Trabajador", "Estado", "¿Reabierto?", "Técnico Anterior", "Motivo Rechazo"};
        modeloPendientes = new DefaultTableModel(colPendientes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaPendientes = new JTable(modeloPendientes);
        panelPendientes.add(new JScrollPane(tablaPendientes), BorderLayout.CENTER);

        JPanel botonesPendientes = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnVerDesc = new JButton("Ver Descripción Completa");
        btnVerDesc.addActionListener(e -> verDetalleTicket(tablaPendientes, modeloPendientes));
        JButton btnTomar = new JButton("Tomar Ticket Seleccionado");
        btnTomar.addActionListener(e -> tomarTicketSeleccionado());
        botonesPendientes.add(btnVerDesc);
        botonesPendientes.add(btnTomar);
        panelPendientes.add(botonesPendientes, BorderLayout.SOUTH);

        // Pestaña 2: Mis tickets en atención (máximo 3)
        JPanel panelAtendidos = new JPanel(new BorderLayout(5, 5));
        String[] colAtendidos = {"ID", "Título", "Trabajador", "Estado", "¿Solicitud Reapertura?", "Nota Resolución"};
        modeloMisTickets = new DefaultTableModel(colAtendidos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaMisTickets = new JTable(modeloMisTickets);
        panelAtendidos.add(new JScrollPane(tablaMisTickets), BorderLayout.CENTER);

        JPanel botonesAtendidos = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnVerDescMio = new JButton("Ver Descripción");
        btnVerDescMio.addActionListener(e -> verDetalleTicket(tablaMisTickets, modeloMisTickets));
        JButton btnResolver = new JButton("Marcar como RESUELTO");
        btnResolver.addActionListener(e -> resolverTicketSeleccionado());
        JButton btnPedirReapertura = new JButton("Solicitar Reapertura al Administrador");
        btnPedirReapertura.addActionListener(e -> solicitarReaperturaSeleccionado());

        botonesAtendidos.add(btnVerDescMio);
        botonesAtendidos.add(btnResolver);
        botonesAtendidos.add(btnPedirReapertura);
        panelAtendidos.add(botonesAtendidos, BorderLayout.SOUTH);

        pestanas.addTab("Tickets Pendientes en el Sistema", panelPendientes);
        pestanas.addTab("Mis Tickets en Atención (Máx. 3)", panelAtendidos);

        add(pestanas, BorderLayout.CENTER);
        actualizarDatos();
    }

    private void verDetalleTicket(JTable tabla, DefaultTableModel modelo) {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un ticket primero.");
            return;
        }
        Ticket t = sistema.buscarTicketPorId((int) modelo.getValueAt(fila, 0));
        if (t != null) {
            String info = "ID: " + t.getIdticket() + "\n"
                    + "Título: " + t.getTitulo() + "\n"
                    + "Creador: " + t.getCreador().getId() + "\n"
                    + "Estado: " + t.getEstado() + "\n\n"
                    + "Descripción:\n" + t.getDescripcion() + "\n\n"
                    + "Motivo Rechazo Previo: " + (t.getMotivoRechazo() != null ? t.getMotivoRechazo() : "Ninguno");
            JOptionPane.showMessageDialog(this, info, "Detalle del Ticket", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void tomarTicketSeleccionado() {
        int fila = tablaPendientes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un ticket pendiente para tomar.");
            return;
        }
        Ticket t = sistema.buscarTicketPorId((int) modeloPendientes.getValueAt(fila, 0));
        try {
            sistema.tomarTicket(tecnico, t);
            actualizarDatos();
            JOptionPane.showMessageDialog(this, "Ticket #" + t.getIdticket() + " asignado. Estado actual: ATENDIDO.");
        } catch (LimiteTicketsException | OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo tomar el ticket", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resolverTicketSeleccionado() {
        int fila = tablaMisTickets.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un ticket de su lista.");
            return;
        }
        Ticket t = sistema.buscarTicketPorId((int) modeloMisTickets.getValueAt(fila, 0));
        String nota = JOptionPane.showInputDialog(this, "Ingrese la explicación/nota de resolución del problema:");
        if (nota == null) return;

        try {
            sistema.resolverTicket(tecnico, t, nota);
            actualizarDatos();
            JOptionPane.showMessageDialog(this, "Ticket marcado como RESUELTO. Queda a la espera de confirmación del trabajador.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarReaperturaSeleccionado() {
        int fila = tablaMisTickets.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un ticket de su lista.");
            return;
        }
        Ticket t = sistema.buscarTicketPorId((int) modeloMisTickets.getValueAt(fila, 0));
        try {
            sistema.solicitarReapertura(tecnico, t);
            actualizarDatos();
            JOptionPane.showMessageDialog(this, "Solicitud de reapertura enviada al Administrador.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarDatos() {
        lblMetricas.setText(String.format("Técnico: %s | Tickets en atención: %d/3 | Fallas: %d/3 | Marcas de retorno: %d",
                tecnico.getId(), tecnico.getTicketsAtendidos().size(), tecnico.getFallas(), tecnico.getMarcasRetorno()));

        modeloPendientes.setRowCount(0);
        List<Ticket> pendientes = tecnico.verTicketsPendientes(sistema);
        for (Ticket t : pendientes) {
            modeloPendientes.addRow(new Object[]{
                    t.getIdticket(),
                    t.getTitulo(),
                    t.getCreador().getId(),
                    t.getEstado(),
                    t.isFueReabierto() ? "Sí" : "No",
                    t.getTecnicoAnterior() != null ? t.getTecnicoAnterior().getId() : "-",
                    t.getMotivoRechazo() != null ? t.getMotivoRechazo() : "-"
            });
        }

        modeloMisTickets.setRowCount(0);
        for (Ticket t : tecnico.getTicketsAtendidos()) {
            boolean enSolicitud = t.isSolicitudReapertura();
            modeloMisTickets.addRow(new Object[]{
                    t.getIdticket(),
                    t.getTitulo(),
                    t.getCreador().getId(),
                    t.getEstado(),
                    enSolicitud ? "Pendiente de Admin" : "No",
                    t.getNotaResolucion() != null ? t.getNotaResolucion() : "-"
            });
        }
    }
}