package com.pukio.posclient.ui.promotion;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.PromotionDto;
import com.pukio.posclient.ui.util.RefreshablePanel;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de gestión de promociones.
 * Visible solo para roles Manager y Administrator.
 * TASK-E2-26o
 */
public class PromotionPanel extends JPanel implements RefreshablePanel {
    private final AppServerClient client;
    
    private JTable promotionTable;
    private PromotionTableModel tableModel;
    
    private JButton newPromotionButton;
    private JButton editButton;
    private JButton refreshButton;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    public PromotionPanel(AppServerClient client) {
        this.client = client;
        initComponents();
        layoutComponents();
        attachListeners();
        loadPromotions();
    }
    
    private void initComponents() {
        tableModel = new PromotionTableModel();
        promotionTable = new JTable(tableModel);
        promotionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promotionTable.setFillsViewportHeight(true);
        promotionTable.setAutoCreateRowSorter(true);
        
        newPromotionButton = new JButton("Nueva Promoción");
        editButton = new JButton("Editar");
        editButton.setEnabled(false);
        refreshButton = new JButton("Actualizar");
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Título
        JLabel titleLabel = new JLabel("Gestión de Promociones");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, BorderLayout.NORTH);
        
        // Tabla
        add(new JScrollPane(promotionTable), BorderLayout.CENTER);
        
        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(newPromotionButton);
        buttonPanel.add(editButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        promotionTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = promotionTable.getSelectedRow() != -1;
            editButton.setEnabled(hasSelection);
        });
        
        newPromotionButton.addActionListener(e -> openPromotionDialog(null));
        editButton.addActionListener(e -> {
            int selectedRow = promotionTable.getSelectedRow();
            if (selectedRow != -1) {
                PromotionDto promotion = tableModel.getPromotionAt(selectedRow);
                openPromotionDialog(promotion);
            }
        });
        refreshButton.addActionListener(e -> refresh());
        
        // Doble clic en fila → editar
        promotionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int selectedRow = promotionTable.getSelectedRow();
                    if (selectedRow != -1) {
                        PromotionDto promotion = tableModel.getPromotionAt(selectedRow);
                        openPromotionDialog(promotion);
                    }
                }
            }
        });
    }
    
    private void loadPromotions() {
        SwingWorkerTask.execute(
            () -> client.getActivePromotions(),
            this::handlePromotionsLoaded,
            this::handleError,
            refreshButton,
            null
        );
    }
    
    private void handlePromotionsLoaded(List<PromotionDto> promotions) {
        tableModel.setPromotions(promotions);
    }
    
    private void handleError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error al cargar promociones: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    private void openPromotionDialog(PromotionDto promotion) {
        PromotionFormDialog dialog = new PromotionFormDialog(
            SwingUtilities.getWindowAncestor(this),
            client,
            promotion
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            refresh();
        }
    }
    
    @Override
    public void refresh() {
        loadPromotions();
    }
    
    // Modelo de tabla
    private static class PromotionTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "Nombre", "Tipo", "Valor", "Vigencia", "Alcance", "Activa"
        };
        private List<PromotionDto> promotions = new ArrayList<>();
        
        public void setPromotions(List<PromotionDto> promotions) {
            this.promotions = new ArrayList<>(promotions);
            fireTableDataChanged();
        }
        
        public PromotionDto getPromotionAt(int row) {
            return promotions.get(row);
        }
        
        @Override
        public int getRowCount() {
            return promotions.size();
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
            PromotionDto promo = promotions.get(rowIndex);
            switch (columnIndex) {
                case 0: return promo.getName();
                case 1: return promo.getType();
                case 2: return formatValue(promo);
                case 3: return formatVigencia(promo);
                case 4: return promo.getScope() != null ? promo.getScope() : "Todas";
                case 5: return promo.isActive() ? "Sí" : "No";
                default: return "";
            }
        }
        
        private String formatValue(PromotionDto promo) {
            if ("PERCENTAGE".equals(promo.getType())) {
                return promo.getValue() + "%";
            } else {
                return String.format("S/. %.2f", promo.getValue());
            }
        }
        
        private String formatVigencia(PromotionDto promo) {
            return promo.getStartDate().format(DATE_FORMATTER) + " - " +
                   promo.getEndDate().format(DATE_FORMATTER);
        }
    }
}
