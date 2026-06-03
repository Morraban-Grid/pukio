package com.pukio.posclient.ui;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.client.SessionContext;
import com.pukio.posclient.ui.sale.ReceiptPanel;
import com.pukio.posclient.ui.sale.SalePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * Main application frame with card layout for different panels.
 * 
 * TASK-E2-26b
 */
public class MainFrame extends JFrame {

    private static final String PREFS_NODE = "pukio/mainframe";
    private static final String PREF_X = "x";
    private static final String PREF_Y = "y";
    private static final String PREF_WIDTH = "width";
    private static final String PREF_HEIGHT = "height";
    
    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 768;

    private final AppServerClient client;
    private final Preferences prefs;

    private HeaderPanel headerPanel;
    private StatusBar statusBar;
    private CardLayout cardLayout;
    private JPanel centerPanel;

    private LoginPanel loginPanel;
    private SalePanel salePanel;
    private ReceiptPanel receiptPanel;

    private JMenuBar menuBar;
    private JMenu ventaMenu;
    private JMenu productosMenu;
    private JMenu inventarioMenu;
    private JMenu arqueoMenu;
    private JMenu promocionesMenu;

    public MainFrame() {
        // Initialize client (will be injected via Spring in full implementation)
        this.client = new AppServerClient(
            new org.springframework.web.client.RestTemplate(),
            System.getProperty("app.server.url", "http://localhost:8080")
        );
        
        this.prefs = Preferences.userRoot().node(PREFS_NODE);

        initComponents();
        setupKeyboardShortcuts();
        restoreWindowState();
    }

    private void initComponents() {
        setTitle("Pukio POS");
        setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        setLayout(new BorderLayout());

        // Header panel
        headerPanel = new HeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Center panel with CardLayout
        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        
        // Create panels
        loginPanel = new LoginPanel(client, this);
        salePanel = new SalePanel(this, client);
        receiptPanel = new ReceiptPanel(this);

        // Add panels to card layout
        centerPanel.add(loginPanel, "login");
        centerPanel.add(salePanel, "sale");
        centerPanel.add(receiptPanel, "receipt");

        add(centerPanel, BorderLayout.CENTER);

        // Status bar
        statusBar = new StatusBar(client);
        add(statusBar, BorderLayout.SOUTH);

        // Create menu bar (initially hidden)
        createMenuBar();
        
        // Show login panel initially
        showPanel("login");
    }

    /**
     * Create the menu bar (visibility controlled by role).
     */
    private void createMenuBar() {
        menuBar = new JMenuBar();

        // Venta menu (always visible after login)
        ventaMenu = new JMenu("Venta");
        JMenuItem ventaItem = new JMenuItem("Procesar Venta");
        ventaItem.addActionListener(e -> showPanel("sale"));
        ventaMenu.add(ventaItem);
        menuBar.add(ventaMenu);

        // Productos menu (MANAGER, ADMINISTRATOR only)
        productosMenu = new JMenu("Productos");
        JMenuItem productosItem = new JMenuItem("Gestionar Productos");
        productosItem.addActionListener(e -> showPanel("products"));
        productosMenu.add(productosItem);
        menuBar.add(productosMenu);

        // Inventario menu (SUPERVISOR, MANAGER, ADMINISTRATOR)
        inventarioMenu = new JMenu("Inventario");
        JMenuItem inventarioItem = new JMenuItem("Ver Inventario");
        inventarioItem.addActionListener(e -> showPanel("inventory"));
        inventarioMenu.add(inventarioItem);
        menuBar.add(inventarioMenu);

        // Arqueo menu (CASHIER, SUPERVISOR, MANAGER)
        arqueoMenu = new JMenu("Arqueo");
        JMenuItem arqueoItem = new JMenuItem("Arqueo de Caja");
        arqueoItem.addActionListener(e -> showPanel("arqueo"));
        arqueoMenu.add(arqueoItem);
        menuBar.add(arqueoMenu);

        // Promociones menu (MANAGER, ADMINISTRATOR only)
        promocionesMenu = new JMenu("Promociones");
        JMenuItem promocionesItem = new JMenuItem("Gestionar Promociones");
        promocionesItem.addActionListener(e -> showPanel("promotions"));
        promocionesMenu.add(promocionesItem);
        menuBar.add(promocionesMenu);

        // Initially hide all menus (shown after login)
        setJMenuBar(null);
    }

    /**
     * Setup global keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F2:
                        // Focus SKU input in SalePanel
                        if (salePanel != null) {
                            salePanel.focusSku();
                        }
                        return true;
                    case KeyEvent.VK_F4:
                        // Open Arqueo panel
                        if (SessionContext.isLoggedIn()) {
                            showPanel("arqueo");
                        }
                        return true;
                    case KeyEvent.VK_F5:
                        // Refresh active panel
                        refreshActivePanel();
                        return true;
                    case KeyEvent.VK_ESCAPE:
                        // Close active modal dialog
                        closeActiveDialog();
                        return true;
                }
            }
            return false;
        });
    }

    /**
     * Show a specific panel by name.
     */
    public void showPanel(String panelName) {
        cardLayout.show(centerPanel, panelName);
    }

    /**
     * Called after successful login.
     */
    public void onLoginSuccess() {
        // Update header
        headerPanel.updateSession(
            SessionContext.getStoreName(),
            SessionContext.getUsername(),
            SessionContext.getShiftId()
        );

        // Update status bar
        statusBar.setRole(SessionContext.getRole());
        statusBar.setLastOperation("Login exitoso");

        // Apply role-based menu visibility
        applyRoleVisibility(SessionContext.getRole());

        // Show menu bar
        setJMenuBar(menuBar);

        // Show sale panel
        showPanel("sale");
    }

    /**
     * Apply role-based menu visibility.
     */
    private void applyRoleVisibility(String role) {
        if (role == null) {
            return;
        }

        // Venta: always visible
        ventaMenu.setVisible(true);

        // Productos: MANAGER, ADMINISTRATOR
        productosMenu.setVisible(
            role.equals("MANAGER") || role.equals("ADMINISTRATOR")
        );

        // Inventario: SUPERVISOR, MANAGER, ADMINISTRATOR
        inventarioMenu.setVisible(
            role.equals("SUPERVISOR") || role.equals("MANAGER") || role.equals("ADMINISTRATOR")
        );

        // Arqueo: CASHIER, SUPERVISOR, MANAGER
        arqueoMenu.setVisible(
            role.equals("CASHIER") || role.equals("SUPERVISOR") || role.equals("MANAGER")
        );

        // Promociones: MANAGER, ADMINISTRATOR
        promocionesMenu.setVisible(
            role.equals("MANAGER") || role.equals("ADMINISTRATOR")
        );
    }

    /**
     * Refresh the currently active panel.
     */
    private void refreshActivePanel() {
        Component activeComponent = getCurrentPanel();
        if (activeComponent instanceof SalePanel) {
            ((SalePanel) activeComponent).refresh();
        }
        // Add other panel refresh methods as they're implemented
    }

    /**
     * Get the currently visible panel.
     */
    private Component getCurrentPanel() {
        for (Component comp : centerPanel.getComponents()) {
            if (comp.isVisible()) {
                return comp;
            }
        }
        return null;
    }

    /**
     * Close the active modal dialog if any.
     */
    private void closeActiveDialog() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog && window.isVisible()) {
                window.dispose();
                break;
            }
        }
    }

    /**
     * Restore window position and size from preferences.
     */
    private void restoreWindowState() {
        int x = prefs.getInt(PREF_X, -1);
        int y = prefs.getInt(PREF_Y, -1);
        int width = prefs.getInt(PREF_WIDTH, DEFAULT_WIDTH);
        int height = prefs.getInt(PREF_HEIGHT, DEFAULT_HEIGHT);

        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null); // Center on screen
        }

        setSize(width, height);
    }

    /**
     * Save window position and size to preferences.
     */
    private void saveWindowState() {
        prefs.putInt(PREF_X, getX());
        prefs.putInt(PREF_Y, getY());
        prefs.putInt(PREF_WIDTH, getWidth());
        prefs.putInt(PREF_HEIGHT, getHeight());
    }

    /**
     * Handle window close event.
     */
    private void onClose() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro que desea cerrar Pukio POS?",
            "Confirmar Cierre",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            saveWindowState();
            headerPanel.stopClock();
            statusBar.stopTimer();
            dispose();
            System.exit(0);
        }
    }

    /**
     * Get the SalePanel instance.
     */
    public SalePanel getSalePanel() {
        return salePanel;
    }

    /**
     * Get the ReceiptPanel instance.
     */
    public ReceiptPanel getReceiptPanel() {
        return receiptPanel;
    }
}
