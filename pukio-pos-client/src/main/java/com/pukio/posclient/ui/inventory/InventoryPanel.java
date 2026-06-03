package com.pukio.posclient.ui.inventory;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.InventoryDto;
import com.pukio.posclient.session.SessionContext;
import com.pukio.posclient.ui.util.RefreshablePanel;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de gestión de inventario con alertas de stock.
 * Visible para roles Supervisor, Manager y Administrator.
 * TASK-E2-26l
 */
public class InventoryPanel extends JPanel implements RefreshablePanel {
    private final AppServerClient client;
    
    private JTable inventoryTable;
    private InventoryTableModel tableModel;
    
    private JButton adjustButton;
    private JButton transferButton;
    private JButton refreshButton;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public InventoryPanel(AppServerClient client) {
        this.client = client;
        initComponents();
        layoutComponents();
        attachListeners();
        loadInventory();
    }
    
    private void initComponents() {
        tableModel = new InventoryTableModel();
        inventoryTable = new JTable(tableModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setAutoCreateRowSorter(true);
        
        // Renderer personalizado para colorear filas según stock
        inventoryTable.setDefaultRenderer(Object.class, new StockCellRenderer());
        
        adjustButton = new JButton("Ajuste Manual");
        transferButton = new JButton("Transferir");
        refreshButton = new JButton("Actualizar");
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Título
        JLabel titleLabel = new JLabel("Inventario de Tienda");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, BorderLayout.NORTH);
        
        // Tabla
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        
        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(adjustButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        adjustButton.addActionListener(e -> openAdjustmentDialog());
        transferButton.addActionListener(e -> openTransferDialog());
        refreshButton.addActionListener(e -> refresh());
        
        // Doble clic en fila → ajuste
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openAdjustmentDialog();
                }
            }
        });
    }
    
    private void loadInventory() {
        String storeId = SessionContext.getStoreId();
        
        SwingWorkerTask.execute(
            () -> client.getInventoryByStore(storeId),
            this::handleInventoryLoaded,
            this::handleError,
            refreshButton,
            null
        );
    }
    
    private void handleInventoryLoaded(List<InventoryDto> inventory) {
        tableModel.setInventory(inventory);
    }
    
    private void handleError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error al cargar inventario: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    private void openAdjustmentDialog() {
        int selectedRow = inventoryTable.getSelectedRow();
        String preFillSku = null;
        
        if (selectedRow != -1) {
            InventoryDto inventory = tableModel.getInventoryAt(selectedRow);
            preFillSku = inventory.getSku();
        }
        
        InventoryAdjustmentDialog dialog = new InventoryAdjustmentDialog(
            SwingUtilities.getWindowAncestor(this),
            client,
            preFillSku
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            refresh();
        }
    }
    
    private void openTransferDialog() {
        TransferDialog dialog = new TransferDialog(
            SwingUtilities.getWindowAncestor(this),
            client
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        if (dialog.isTransferred()) {
            refresh();
        }
    }
    
    @Override
    public void refresh() {
        loadInventory();
    }
    
    // Modelo de tabla
    private static class InventoryTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "SKU", "Nombre", "Stock Actual", "Punto Reorden", "Tienda", "Última Actualización"
        };
        private List<InventoryDto> inventory = new ArrayList<>();
        
        public void setInventory(List<InventoryDto> inventory) {
            this.inventory = new ArrayList<>(inventory);
            fireTableDataChanged();
        }
        
        public InventoryDto getInventoryAt(int row) {
            return inventory.get(row);
        }
        
        @Override
        public int getRowCount() {
            return inventory.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            InventoryDto inv = inventory.get(rowIndex);
            switch (columnIndex) {
                case 0: return inv.getSku();
                case 1: return inv.getProductName();
                case 2: return inv.getQuantity();
                case 3: return inv.getReorderPoint();
                case 4: return inv.getStoreName();
                case 5: return inv.getLastUpdated() != null ? 
                              inv.getLastUpdated().format(DATE_FORMATTER) : "-";
                default: return "";
            }
        }
    }
    
    // Renderer para colorear filas según nivel de stock
    private static class StockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                InventoryTableModel model = (InventoryTableModel) table.getModel();
                InventoryDto inv = model.getInventoryAt(row);
                
                if (inv.getQuantity() == 0) {
                    // Stock agotado → rojo
                    c.setBackground(new Color(255, 200, 200));
                    c.setForeground(Color.BLACK);
                } else if (inv.getQuantity() <= inv.getReorderPoint()) {
                    // Stock en punto de reorden → amarillo
                    c.setBackground(new Color(255, 255, 200));
                    c.setForeground(Color.BLACK);
                } else {
                    // Stock normal → blanco
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            
            return c;
        }
    }
}
