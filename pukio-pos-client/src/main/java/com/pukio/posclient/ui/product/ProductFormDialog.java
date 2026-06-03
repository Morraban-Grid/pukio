package com.pukio.posclient.ui.product;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.ProductDto;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.regex.Pattern;

/**
 * Diálogo modal para crear/editar productos.
 * TASK-E2-26k
 */
public class ProductFormDialog extends JDialog {
    private final AppServerClient client;
    private final ProductDto product; // null = creación, no-null = edición
    private boolean saved = false;
    
    private JTextField skuField;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextField priceField;
    private JComboBox<String> categoryCombo;
    private JLabel imageLabel;
    private JButton selectImageButton;
    
    private JButton saveButton;
    private JButton cancelButton;
    
    private byte[] imageBytes;
    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    
    public ProductFormDialog(Window owner, AppServerClient client, ProductDto product) {
        super(owner, product == null ? "Nuevo Producto" : "Editar Producto", ModalityType.APPLICATION_MODAL);
        this.client = client;
        this.product = product;
        
        initComponents();
        layoutComponents();
        attachListeners();
        
        if (product != null) {
            populateFields();
        }
        
        pack();
        setResizable(false);
    }
    
    private void initComponents() {
        skuField = new JTextField(20);
        skuField.setEditable(product == null); // Solo editable en creación
        
        nameField = new JTextField(30);
        
        descriptionArea = new JTextArea(3, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        priceField = new JTextField(15);
        
        categoryCombo = new JComboBox<>(new String[]{
            "Alimentos",
            "Bebidas",
            "Limpieza",
            "Cuidado Personal",
            "Snacks"
        });
        
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(80, 80));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setText("Sin imagen");
        
        selectImageButton = new JButton("Seleccionar Imagen");
        
        saveButton = new JButton("Guardar");
        cancelButton = new JButton("Cancelar");
    }
    
    private void layoutComponents() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // SKU
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel("SKU:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(skuField, gbc);
        
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(nameField, gbc);
        
        // Descripción
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        contentPanel.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(new JScrollPane(descriptionArea), gbc);
        
        // Precio
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(new JLabel("Precio (S/.):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(priceField, gbc);
        
        // Categoría
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(categoryCombo, gbc);
        
        // Imagen
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        contentPanel.add(new JLabel("Imagen:"), gbc);
        
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imagePanel.add(imageLabel);
        imagePanel.add(selectImageButton);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(imagePanel, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        // Validación de precio en tiempo real
        priceField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validatePrice();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                validatePrice();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                validatePrice();
            }
        });
        
        selectImageButton.addActionListener(e -> selectImage());
        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());
        
        // Enter en campos de texto → guardar
        nameField.addActionListener(e -> save());
        priceField.addActionListener(e -> save());
    }
    
    private void validatePrice() {
        String text = priceField.getText().trim();
        if (text.isEmpty()) {
            priceField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            return;
        }
        
        if (PRICE_PATTERN.matcher(text).matches()) {
            priceField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        } else {
            priceField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
    }
    
    private void populateFields() {
        skuField.setText(product.getSku());
        nameField.setText(product.getName());
        descriptionArea.setText(product.getDescription());
        priceField.setText(product.getPrice().toString());
        
        // Seleccionar categoría
        for (int i = 0; i < categoryCombo.getItemCount(); i++) {
            if (categoryCombo.getItemAt(i).equals(product.getCategory())) {
                categoryCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Cargar imagen si existe
        // (En producción, esto cargaría la imagen desde el servidor)
        if (product.getImageUrl() != null) {
            imageLabel.setText("Imagen actual");
        }
    }
    
    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                imageBytes = Files.readAllBytes(selectedFile.toPath());
                
                // Mostrar miniatura
                ImageIcon icon = new ImageIcon(imageBytes);
                Image scaledImage = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.setText(null);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al cargar la imagen: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private void save() {
        // Validaciones
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceText = priceField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El SKU es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
            skuField.requestFocus();
            return;
        }
        
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        if (priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El precio es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
            priceField.requestFocus();
            return;
        }
        
        if (!PRICE_PATTERN.matcher(priceText).matches()) {
            JOptionPane.showMessageDialog(this, "El precio debe ser un número decimal positivo", "Validación", JOptionPane.WARNING_MESSAGE);
            priceField.requestFocus();
            return;
        }
        
        BigDecimal price = new BigDecimal(priceText);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "El precio debe ser mayor que cero", "Validación", JOptionPane.WARNING_MESSAGE);
            priceField.requestFocus();
            return;
        }
        
        // Crear DTO
        ProductDto dto = new ProductDto();
        dto.setSku(sku);
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setCategory(category);
        dto.setImageBytes(imageBytes);
        
        // Guardar
        SwingWorkerTask.execute(
            () -> {
                if (product == null) {
                    client.createProduct(dto);
                } else {
                    client.updateProduct(dto);
                }
                return null;
            },
            result -> {
                saved = true;
                dispose();
            },
            ex -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al guardar el producto: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            saveButton,
            null
        );
    }
    
    public boolean isSaved() {
        return saved;
    }
}
