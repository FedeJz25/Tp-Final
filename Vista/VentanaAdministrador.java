package Vista;

import Modelo.*;
import Excepciones.OperacionTicketException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VentanaAdministrador extends JFrame {
    private Sistema sistema;
    private Administrador admin;

    private JTable tablaUsuarios;
    private DefaultTableModel modeloUsuarios;

    private JTable tablaSolicitudes;
    private DefaultTableModel modeloSolicitudes;

    private JTable tablaTickets;
    private DefaultTableModel modeloTickets;
    private JComboBox<String> comboFiltroEstado;

    public VentanaAdministrador(Sistema sistema, Administrador admin) {
        this.sistema = sistema;
        this.admin = admin;

        setTitle("Panel de Administración - ID: " + admin.getId());
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Barra superior
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panelSuperior.add(new JLabel("Administrador conectado: " + admin.getId()), BorderLayout.WEST);

        JPanel panelBotonesSup = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCambiarClave = new JButton("Cambiar Contraseña");
        btnCambiarClave.addActionListener(e -> VentanaLogin.mostrarDialogoCambioClave(this, sistema, admin));
        JButton btnSalir = new JButton("Cerrar Sesión");
        btnSalir.addActionListener(e -> {
            new VentanaLogin(sistema).setVisible(true);
            dispose();
        });
        panelBotonesSup.add(btnCambiarClave);
        panelBotonesSup.add(btnSalir);
        panelSuperior.add(panelBotonesSup, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("1. Usuarios y Métricas de Técnicos", crearPanelUsuarios());
        pestanas.addTab("2. Solicitudes de Reapertura", crearPanelSolicitudes());
        pestanas.addTab("3. Auditoría de Todos los Tickets", crearPanelAuditoriaTickets());

        add(pestanas, BorderLayout.CENTER);
        actualizarTodo();
    }

    private JPanel crearPanelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Alta de usuarios
        JPanel panelAlta = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelAlta.setBorder(BorderFactory.createTitledBorder("Dar de Alta Nuevo Usuario (Clave inicial = ID)"));
        JTextField txtNuevoId = new JTextField(12);
        JComboBox<String> comboRol = new JComboBox<>(new String[]{"Trabajador", "Técnico", "Administrador"});
        JButton btnCrearUsuario = new JButton("Crear Usuario");

        btnCrearUsuario.addActionListener(e -> {
            String id = txtNuevoId.getText().trim();
            String rol = (String) comboRol.getSelectedItem();
            Usuario nuevo;
            if ("Técnico".equals(rol)) {
                nuevo = new Tecnico(id);
            } else if ("Administrador".equals(rol)) {
                nuevo = new Administrador(id);
            } else {
                nuevo = new Trabajador(id);
            }

            try {
                sistema.agregarUsuario(admin, nuevo);
                txtNuevoId.setText("");
                actualizarTodo();
                JOptionPane.showMessageDialog(this, "Usuario creado con éxito. Su contraseña inicial es: " + id);
            } catch (OperacionTicketException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panelAlta.add(new JLabel("ID :"));
        panelAlta.add(txtNuevoId);
        panelAlta.add(new JLabel("Rol:"));
        panelAlta.add(comboRol);
        panelAlta.add(btnCrearUsuario);
        panel.add(panelAlta, BorderLayout.NORTH);

        // Tabla de usuarios y métricas de técnicos
        String[] cols = {"ID", "Rol", "Estado Acceso", "¿Cambio Clave Pendiente?", "Fallas (Técnico)", "Marcas Retorno (Técnico)", "Tickets en Atención"};
        modeloUsuarios = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaUsuarios = new JTable(modeloUsuarios);
        panel.add(new JScrollPane(tablaUsuarios), BorderLayout.CENTER);

        // Acciones sobre usuario seleccionado
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnBloquear = new JButton("Bloquear Usuario");
        btnBloquear.addActionListener(e -> cambiarBloqueoUsuario(true));

        JButton btnDesbloquear = new JButton("Desbloquear Usuario");
        btnDesbloquear.addActionListener(e -> cambiarBloqueoUsuario(false));

        JButton btnBlanquear = new JButton("Blanquear Contraseña (Clave = ID)");
        btnBlanquear.addActionListener(e -> blanquearClaveSeleccionado());

        JButton btnReasignar = new JButton("Liberar Tickets Congelados (Técnico Bloqueado)");
        btnReasignar.addActionListener(e -> reasignarTicketsDeTecnico());

        panelAcciones.add(btnBloquear);
        panelAcciones.add(btnDesbloquear);
        panelAcciones.add(btnBlanquear);
        panelAcciones.add(btnReasignar);
        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelSolicitudes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID Ticket", "Título", "Trabajador Emisor", "Técnico Solicitante", "Marcas Actuales del Técnico", "Fallas Actuales"};
        modeloSolicitudes = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaSolicitudes = new JTable(modeloSolicitudes);
        panel.add(new JScrollPane(tablaSolicitudes), BorderLayout.CENTER);

        JButton btnAprobar = new JButton("Aprobar Reapertura de Ticket Seleccionado");
        btnAprobar.addActionListener(e -> {
            int fila = tablaSolicitudes.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una solicitud de la tabla.");
                return;
            }
            Ticket t = sistema.buscarTicketPorId((int) modeloSolicitudes.getValueAt(fila, 0));
            try {
                sistema.aprobarReaperturaTicket(admin, t);
                actualizarTodo();
                JOptionPane.showMessageDialog(this, "Ticket reabierto con éxito. Se computó la marca de retorno/falla correspondiente al técnico.");
            } catch (OperacionTicketException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(btnAprobar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelAuditoriaTickets() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.add(new JLabel("Filtrar por Estado:"));
        comboFiltroEstado = new JComboBox<>(new String[]{
                "TODOS",
                EstadoTicket.NO_ATENDIDO.name(),
                EstadoTicket.ATENDIDO.name(),
                EstadoTicket.RESUELTO.name(),
                EstadoTicket.REABIERTO.name(),
                EstadoTicket.FINALIZADO.name()
        });
        comboFiltroEstado.addActionListener(e -> actualizarTablaTickets());
        panelFiltro.add(comboFiltroEstado);
        panel.add(panelFiltro, BorderLayout.NORTH);

        String[] cols = {"ID", "Título", "Trabajador Creador", "Estado", "Técnico Actual", "¿Fue Reabierto?", "Técnico Anterior"};
        modeloTickets = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaTickets = new JTable(modeloTickets);
        panel.add(new JScrollPane(tablaTickets), BorderLayout.CENTER);

        JButton btnDetalle = new JButton("Ver Información Detallada del Ticket Seleccionado");
        btnDetalle.addActionListener(e -> {
            int fila = tablaTickets.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione un ticket de la tabla.");
                return;
            }
            Ticket t = sistema.buscarTicketPorId((int) modeloTickets.getValueAt(fila, 0));
           if (t != null) {
                // 1. Formateamos la fecha para que quede prolija (ej: 15/10/2026 19:05)
                String fecha = t.getFechaTicket().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                // 2. La agregamos a la derecha de ID Ticket en la primera línea
                String detalle = "ID Ticket: " + t.getIdticket() + "                    Fecha: " + fecha + "\n"
                        + "Título: " + t.getTitulo() + "\n"
                        + "Descripción: " + t.getDescripcion() + "\n"
                        + "Trabajador que lo creó: " + t.getCreador().getId() + "\n"
                        + "Estado actual: " + t.getEstado() + "\n"
                        + "Técnico actual: " + (t.getTecnicoActual() != null ? t.getTecnicoActual().getId() : "Ninguno") + "\n"
                        + "¿Fue reabierto?: " + (t.isFueReabierto() ? "Sí" : "No") + "\n"
                        + "Técnico que lo atendió antes: " + (t.getTecnicoAnterior() != null ? t.getTecnicoAnterior().getId() : "Ninguno") + "\n"
                        + "Nota de resolución: " + (t.getNotaResolucion() != null ? t.getNotaResolucion() : "-") + "\n"
                        + "Motivo de rechazo: " + (t.getMotivoRechazo() != null ? t.getMotivoRechazo() : "-");
                JOptionPane.showMessageDialog(this, detalle, "Auditoría de Ticket #" + t.getIdticket(), JOptionPane.INFORMATION_MESSAGE);
            }
        }); 
        panel.add(btnDetalle, BorderLayout.SOUTH);

        return panel;
    }

    private void cambiarBloqueoUsuario(boolean bloquear) {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.");
            return;
        }
        String idUsuario = (String) modeloUsuarios.getValueAt(fila, 0);
        try {
            if (bloquear) {
                sistema.bloquearUsuario(admin, idUsuario);
                JOptionPane.showMessageDialog(this, "Usuario " + idUsuario + " bloqueado.");
            } else {
                sistema.desbloquearUsuario(admin, idUsuario);
                JOptionPane.showMessageDialog(this, "Usuario " + idUsuario + " desbloqueado y habilitado.");
            }
            actualizarTodo();
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void blanquearClaveSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.");
            return;
        }
        String idUsuario = (String) modeloUsuarios.getValueAt(fila, 0);
        try {
            sistema.blanquearContrasena(admin, idUsuario);
            actualizarTodo();
            JOptionPane.showMessageDialog(this, "Contraseña blanqueada. Ahora la clave de " + idUsuario + " es igual a su ID y deberá cambiarla al iniciar sesión.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reasignarTicketsDeTecnico() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un técnico bloqueado de la tabla.");
            return;
        }
        String idUsuario = (String) modeloUsuarios.getValueAt(fila, 0);
        Usuario u = sistema.buscarUsuario(idUsuario);
        if (!(u instanceof Tecnico)) {
            JOptionPane.showMessageDialog(this, "El usuario seleccionado no es un Técnico.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            admin.reasignarTicketsCongelados(sistema, (Tecnico) u);
            actualizarTodo();
            JOptionPane.showMessageDialog(this, "Los tickets del técnico bloqueado pasaron a estado REABIERTO y quedaron libres.");
        } catch (OperacionTicketException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actualizarTodo() {
        modeloUsuarios.setRowCount(0);
        for (Usuario u : sistema.getUsuarios()) {
            String rol = (u instanceof Administrador) ? "Administrador" : (u instanceof Tecnico) ? "Técnico" : "Trabajador";
            String fallas = "-";
            String marcas = "-";
            String activos = "-";
            if (u instanceof Tecnico) {
                Tecnico t = (Tecnico) u;
                fallas = String.valueOf(t.getFallas());
                marcas = String.valueOf(t.getMarcasRetorno());
                activos = String.valueOf(t.getTicketsAtendidos().size());
            }
            modeloUsuarios.addRow(new Object[]{
                    u.getId(),
                    rol,
                    u.isBloqueado() ? "BLOQUEADO" : "Activo",
                    u.isRequiereCambioClave() ? "Sí" : "No",
                    fallas,
                    marcas,
                    activos
            });
        }

        modeloSolicitudes.setRowCount(0);
        for (Ticket t : sistema.getSolicitudesReapertura()) {
            Tecnico tec = t.getTecnicoActual();
            modeloSolicitudes.addRow(new Object[]{
                    t.getIdticket(),
                    t.getTitulo(),
                    t.getCreador().getId(),
                    tec != null ? tec.getId() : "-",
                    tec != null ? tec.getMarcasRetorno() : 0,
                    tec != null ? tec.getFallas() : 0
            });
        }

        actualizarTablaTickets();
    }

    private void actualizarTablaTickets() {
        modeloTickets.setRowCount(0);
        String seleccion = (String) comboFiltroEstado.getSelectedItem();
        List<Ticket> lista;
        if (seleccion == null || "TODOS".equals(seleccion)) {
            lista = admin.auditarTodosLosTickets(sistema);
        } else {
            lista = sistema.listarTicketsPorEstado(EstadoTicket.valueOf(seleccion));
        }

        for (Ticket t : lista) {
            modeloTickets.addRow(new Object[]{
                    t.getIdticket(),
                    t.getTitulo(),
                    t.getCreador().getId(),
                    t.getEstado(),
                    t.getTecnicoActual() != null ? t.getTecnicoActual().getId() : "Sin asignar",
                    t.isFueReabierto() ? "Sí" : "No",
                    t.getTecnicoAnterior() != null ? t.getTecnicoAnterior().getId() : "Ninguno"
            });
        }
    }
}