package com.pukio.posclient.ui.product;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.ProductDto;
import com.pukio.posclient.dto.Page;
import com.pukio.posclient.ui.common.SwingWorkerTask;
import com.pukio.posclient.ui.util.RefreshablePanel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de gestión de productos con búsqueda, paginación y CRUD.
 * Visible solo para roles Manager y Administrator.
 * TASK-E2-26j
 */
public class ProductPanel extends JPanel implements RefreshablePanel {
    private final AppServerClient client;
    
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JButton searchButton;
    
    private JTable productTable;
    private ProductTableModel tableModel;
    
    private JButton previousButton;
    private JLabel pageLabel;
    private JButton nextButton;
    
    private JButton newProductButton;
    private JButton editButton;
    private JButton deactivateButton;
    
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 50;
    
    public ProductPanel(AppServerClient client) {
        this.client = client;
        initComponents();
        layoutComponents();
        attachListeners();
        loadPage(0);
    }
    
    private void initComponents() {
        // Barra de búsqueda
        searchField = new JTextField(20);
        categoryCombo = new JComboBox<>(new String[]{
            "Todas",
            "Alimentos",
            "Bebidas",
            "Limpieza",
            "Cuidado Personal",
            "Snacks"
        });
        searchButton = new JButton("Buscar");
        
        // Tabla de productos
        tableModel = new ProductTableModel();
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setFillsViewportHeight(true);
        productTable.setAutoCreateRowSorter(true);
        
        // Paginación
        previousButton = new JButton("Anterior");
        previousButton.setEnabled(false);
        pageLabel = new JLabel("Página 1 de 1");
        nextButton = new JButton("Siguiente");
        nextButton.setEnabled(false);
        
        // Botones de acción
        newProductButton = new JButton("Nuevo Producto");
        editButton = new JButton("Editar");
        editButton.setEnabled(false);
        deactivateButton = new JButton("Desactivar");
        deactivateButton.setEnabled(false);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior de búsqueda
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Buscar:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Categoría:"));
        searchPanel.add(categoryCombo);
        searchPanel.add(searchButton);
        
        add(searchPanel, BorderLayout.NORTH);
        
        // Panel central con la tabla
        add(new JScrollPane(productTable), BorderLayout.CENTER);
        
        // Panel inferior con paginación y botones
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Paginación
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        paginationPanel.add(previousButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextButton);
        bottomPanel.add(paginationPanel, BorderLayout.NORTH);
        
        // Botones de acción
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(newProductButton);
        actionPanel.add(editButton);
        actionPanel.add(deactivateButton);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        // Búsqueda
        searchButton.addActionListener(e -> loadPage(0));
        searchField.addActionListener(e -> loadPage(0));
        categoryCombo.addActionListener(e -> {
            if (categoryCombo.getSelectedIndex() > 0) {
                loadPage(0);
            }
        });
        
        // Paginación
        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                loadPage(currentPage - 1);
            }
        });
        
        nextButton.addActionListener(e -> {
            if (currentPage < totalPages - 1) {
                loadPage(currentPage + 1);
            }
        });
        
        // Selección de fila
        productTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = productTable.getSelectedRow() != -1;
            editButton.setEnabled(hasSelection);
            deactivateButton.setEnabled(hasSelection);
        });
        
        // Botones de acción
        newProductButton.addActionListener(e -> openProductDialog(null));
        editButton.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow != -1) {
                ProductDto product = tableModel.getProductAt(selectedRow);
                openProductDialog(product);
            }
        });
        deactivateButton.addActionListener(e -> deactivateProduct());
    }
    
    private void loadPage(int page) {
        String searchText = searchField.getText().trim();
        String category = categoryCombo.getSelectedIndex() == 0 ? null : 
                         (String) categoryCombo.getSelectedItem();
        
        SwingWorkerTask.execute(
            () -> client.getProducts(searchText, category, page),
            this::handlePageLoaded,
            this::handleError,
            searchButton,
            null
        );
    }
    
    private void handlePageLoaded(Page<ProductDto> page) {
        currentPage = page.getNumber();
        totalPages = page.getTotalPages();
        
        tableModel.setProducts(page.getContent());
        
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
        pageLabel.setText("Página " + (currentPage + 1) + " de " + totalPages);
    }
    
    private void handleError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error al cargar productos: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    private void openProductDialog(ProductDto product) {
        ProductFormDialog dialog = new ProductFormDialog(
            SwingUtilities.getWindowAncestor(this),
            client,
            product
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        // Refrescar si el diálogo guardó cambios
        if (dialog.isSaved()) {
            refresh();
        }
    }
    
    private void deactivateProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        ProductDto product = tableModel.getProductAt(selectedRow);
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea desactivar el producto " + product.getName() + "?",
            "Confirmar desactivación",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorkerTask.execute(
                () -> {
                    client.deactivateProduct(product.getSku());
                    return null;
                },
                result -> {
                    JOptionPane.showMessageDialog(
                        this,
                        "Producto desactivado exitosamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    refresh();
                },
                this::handleError,
                deactivateButton,
                null
            );
        }
    }
    
    @Override
    public void refresh() {
        loadPage(currentPage);
    }
    
    // Modelo de tabla para productos
    private static class ProductTableModel extends AbstractTableModel {
        private final String[] columnNames = {"SKU", "Nombre", "Categoría", "Precio", "Stock", "Estado"};
        private List<ProductDto> products = new ArrayList<>();
        
        public void setProducts(List<ProductDto> products) {
            this.products = new ArrayList<>(products);
            fireTableDataChanged();
        }
        
        public ProductDto getProductAt(int row) {
            return products.get(row);
        }
        
        @Override
        public int getRowCount() {
            return products.size();
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
            ProductDto product = products.get(rowIndex);
            switch (columnIndex) {
                case 0: return product.getSku();
                case 1: return product.getName();
                case 2: return product.getCategory();
                case 3: return String.format("S/. %.2f", product.getPrice());
                case 4: return product.getStock();
                case 5: return product.isActive() ? "Activo" : "Inactivo";
                default: return "";
            }
        }
    }
}
