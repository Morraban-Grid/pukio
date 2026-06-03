package com.pukio.posclient.ui.arqueo;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.ArqueoRequestDto;
import com.pukio.posclient.dto.ArqueoResult;
import com.pukio.posclient.dto.ArqueoSummaryDto;
import com.pukio.posclient.dto.ArqueoSummaryResponse;
import com.pukio.posclient.session.SessionContext;
import com.pukio.posclient.ui.util.RefreshablePanel;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel de arqueo de caja con conciliación automática.
 * TASK-E2-26n
 */
public class ArqueoPanel extends JPanel implements RefreshablePanel {
    private final AppServerClient client;
    
    private JTable expectedTable;
    private ExpectedTableModel expectedTableModel;
    
    private Map<String, JTextField> declaredFields = new HashMap<>();
    private Map<String, JLabel> varianceLabels = new HashMap<>();
    
    private JButton closeShiftButton;
    private JButton refreshButton;
    
    private ArqueoSummaryResponse expectedData;
    private static final BigDecimal VARIANCE_THRESHOLD = new BigDecimal("50.00");
    
    private static final String[] PAYMENT_METHODS = {
        "Efectivo",
        "Tarjeta Crédito",
        "Tarjeta Débito",
        "Transferencia",
        "Billetera Digital"
    };
    
    public ArqueoPanel(AppServerClient client) {
        this.client = client;
        initComponents();
        layoutComponents();
        attachListeners();
        loadExpectedAmounts();
    }
    
    private void initComponents() {
        expectedTableModel = new ExpectedTableModel();
        expectedTable = new JTable(expectedTableModel);
        expectedTable.setFillsViewportHeight(true);
        
        closeShiftButton = new JButton("Cerrar Turno");
        refreshButton = new JButton("Actualizar");
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Título
        JLabel titleLabel = new JLabel("Arqueo de Caja");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, BorderLayout.NORTH);
        
        // Panel central con tabla y formulario
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Tabla de montos esperados (solo lectura)
        JPanel expectedPanel = new JPanel(new BorderLayout());
        expectedPanel.setBorder(BorderFactory.createTitledBorder("Montos Esperados"));
        expectedPanel.add(new JScrollPane(expectedTable), BorderLayout.CENTER);
        centerPanel.add(expectedPanel);
        
        // Formulario de declaración
        JPanel declaredPanel = new JPanel(new GridBagLayout());
        declaredPanel.setBorder(BorderFactory.createTitledBorder("Montos Declarados"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        for (String method : PAYMENT_METHODS) {
            gbc.gridx = 0;
            gbc.gridy = row;
            declaredPanel.add(new JLabel(method + ":"), gbc);
            
            gbc.gridx = 1;
            JTextField declaredField = new JTextField(10);
            declaredField.setText("0.00");
            declaredFields.put(method, declaredField);
            declaredPanel.add(declaredField, gbc);
            
            gbc.gridx = 2;
            declaredPanel.add(new JLabel("Varianza:"), gbc);
            
            gbc.gridx = 3;
            JLabel varianceLabel = new JLabel("S/. 0.00");
            varianceLabels.put(method, varianceLabel);
            declaredPanel.add(varianceLabel, gbc);
            
            row++;
        }
        
        centerPanel.add(declaredPanel);
        add(centerPanel, BorderLayout.CENTER);
        
        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeShiftButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        // Listener para recalcular varianza en tiempo real
        DocumentListener varianceListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                calculateVariances();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                calculateVariances();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                calculateVariances();
            }
        };
        
        for (JTextField field : declaredFields.values()) {
            field.getDocument().addDocumentListener(varianceListener);
        }
        
        closeShiftButton.addActionListener(e -> closeShift());
        refreshButton.addActionListener(e -> refresh());
    }
    
    private void loadExpectedAmounts() {
        String storeId = SessionContext.getStoreId();
        String shiftId = SessionContext.getShiftId();
        
        SwingWorkerTask.execute(
            () -> client.getExpectedAmounts(storeId, shiftId),
            this::handleExpectedLoaded,
            this::handleError,
            refreshButton,
            null
        );
    }
    
    private void handleExpectedLoaded(ArqueoSummaryResponse expected) {
        this.expectedData = expected;
        expectedTableModel.setData(expected);
        calculateVariances();
    }
    
    private void handleError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    private void calculateVariances() {
        if (expectedData == null) return;
        
        Map<String, BigDecimal> expected = expectedData.getExpectedAmountsByMethod();
        
        for (String method : PAYMENT_METHODS) {
            try {
                String declaredText = declaredFields.get(method).getText().trim();
                BigDecimal declared = new BigDecimal(declaredText);
                BigDecimal expectedAmount = expected.getOrDefault(method, BigDecimal.ZERO);
                BigDecimal variance = declared.subtract(expectedAmount);
                
                JLabel varianceLabel = varianceLabels.get(method);
                varianceLabel.setText(String.format("S/. %.2f", variance));
                
                // Colorear en rojo si excede umbral
                if (variance.abs().compareTo(VARIANCE_THRESHOLD) > 0) {
                    varianceLabel.setForeground(Color.RED);
                } else {
                    varianceLabel.setForeground(Color.BLACK);
                }
            } catch (NumberFormatException e) {
                varianceLabels.get(method).setText("Error");
                varianceLabels.get(method).setForeground(Color.RED);
            }
        }
    }
    
    private void closeShift() {
        if (expectedData == null) {
            JOptionPane.showMessageDialog(
                this,
                "Debe cargar los montos esperados primero",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // Validar que todos los campos tengan valores numéricos
        Map<String, BigDecimal> declared = new HashMap<>();
        try {
            for (Map.Entry<String, JTextField> entry : declaredFields.entrySet()) {
                String text = entry.getValue().getText().trim();
                declared.put(entry.getKey(), new BigDecimal(text));
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this,
                "Todos los montos deben ser valores numéricos válidos",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        ArqueoRequestDto request = new ArqueoRequestDto();
        request.setStoreId(SessionContext.getStoreId());
        request.setShiftId(SessionContext.getShiftId());
        request.setCashierId(SessionContext.getUserId());
        request.setDeclaredAmounts(declared);
        
        SwingWorkerTask.execute(
            () -> client.submitArqueo(request),
            this::handleArqueoCompleted,
            this::handleError,
            closeShiftButton,
            null
        );
    }
    
    private void handleArqueoCompleted(ArqueoResult response) {
        String message = "Arqueo completado exitosamente\n" +
                        "Estado: " + response.getStatus() + "\n" +
                        "ID: " + response.getArqueoId();
        
        if ("PENDIENTE_APROBACION".equals(response.getStatus())) {
            message += "\n\nLa varianza excede el umbral.\n" +
                      "Se requiere aprobación de supervisor.";
        }
        
        JOptionPane.showMessageDialog(
            this,
            message,
            "Arqueo Completado",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        // Limpiar formulario
        for (JTextField field : declaredFields.values()) {
            field.setText("0.00");
        }
        
        refresh();
    }
    
    @Override
    public void refresh() {
        loadExpectedAmounts();
    }
    
    // Modelo de tabla para montos esperados
    private static class ExpectedTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Método de Pago", "Monto Esperado"};
        private ArqueoSummaryResponse data;
        
        public void setData(ArqueoSummaryResponse data) {
            this.data = data;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return data != null ? PAYMENT_METHODS.length : 0;
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
            if (data == null) return "";
            
            String method = PAYMENT_METHODS[rowIndex];
            switch (columnIndex) {
                case 0: return method;
                case 1:
                    BigDecimal amount = data.getExpectedAmountsByMethod().getOrDefault(method, BigDecimal.ZERO);
                    return String.format("S/. %.2f", amount);
                default: return "";
            }
        }
    }
}
