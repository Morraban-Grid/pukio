package com.pukio.posclient.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Header panel showing store info, cashier, and real-time clock.
 * 
 * TASK-E2-26c
 */
public class HeaderPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JLabel storeLabel;
    private final JLabel clockLabel;
    private final JLabel shiftLabel;
    private final Timer clockTimer;

    public HeaderPanel() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        setBackground(new Color(240, 240, 240));

        // Left: Store and cashier info
        storeLabel = new JLabel("Tienda: -- | Cajero: --");
        storeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(storeLabel, BorderLayout.WEST);

        // Center: Real-time clock
        clockLabel = new JLabel("--:--:--");
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        clockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(clockLabel, BorderLayout.CENTER);

        // Right: Shift info
        shiftLabel = new JLabel("Turno: --");
        shiftLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        shiftLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(shiftLabel, BorderLayout.EAST);

        // Start clock timer (update every second)
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock(); // Initial update
    }

    /**
     * Update session information after login.
     */
    public void updateSession(String storeName, String cashierName, String shiftId) {
        storeLabel.setText("Tienda: " + storeName + " | Cajero: " + cashierName);
        shiftLabel.setText("Turno: " + shiftId);
    }

    /**
     * Update the clock display.
     */
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(TIME_FORMATTER);
        String dateStr = now.format(DATE_FORMATTER);
        clockLabel.setText(dateStr + " " + timeStr);
    }

    /**
     * Stop the clock timer when panel is removed.
     */
    public void stopClock() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }
}
