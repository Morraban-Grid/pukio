package com.pukio.posclient.ui.util;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generic SwingWorker utility for executing tasks in background.
 * Handles button disabling, progress bar, and callbacks.
 * 
 * TASK-E2-26q
 */
public class SwingWorkerTask<T> extends SwingWorker<T, Void> {

    private final Supplier<T> backgroundTask;
    private final Consumer<T> onSuccess;
    private final Consumer<Exception> onError;
    private final JButton buttonToDisable;
    private final JProgressBar progressBar;

    public SwingWorkerTask(Supplier<T> backgroundTask,
                          Consumer<T> onSuccess,
                          Consumer<Exception> onError,
                          JButton buttonToDisable,
                          JProgressBar progressBar) {
        this.backgroundTask = backgroundTask;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.buttonToDisable = buttonToDisable;
        this.progressBar = progressBar;
    }

    @Override
    protected T doInBackground() throws Exception {
        // Disable button and show progress bar on EDT (already on worker thread)
        SwingUtilities.invokeLater(() -> {
            if (buttonToDisable != null) {
                buttonToDisable.setEnabled(false);
            }
            if (progressBar != null) {
                progressBar.setVisible(true);
                progressBar.setIndeterminate(true);
            }
        });

        // Execute the background task
        return backgroundTask.get();
    }

    @Override
    protected void done() {
        // Re-enable button and hide progress bar
        if (buttonToDisable != null) {
            buttonToDisable.setEnabled(true);
        }
        if (progressBar != null) {
            progressBar.setVisible(false);
            progressBar.setIndeterminate(false);
        }

        // Handle result or exception
        try {
            T result = get();
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        } catch (Exception e) {
            if (onError != null) {
                // Extract the root cause
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                onError.accept(cause instanceof Exception ? (Exception) cause : e);
            }
        }
    }
}
