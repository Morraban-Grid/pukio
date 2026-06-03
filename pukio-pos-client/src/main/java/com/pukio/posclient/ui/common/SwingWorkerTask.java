package com.pukio.posclient.ui.common;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for executing background tasks with SwingWorker.
 * Handles common patterns: disable button, show progress, execute task,
 * handle success/error callbacks, re-enable button.
 *
 * @param <T> The result type of the background task
 */
public class SwingWorkerTask<T> {
    
    private final Supplier<T> backgroundTask;
    private final Consumer<T> onSuccess;
    private final Consumer<Exception> onError;
    private final JButton buttonToDisable;
    private final JProgressBar progressBar;
    
    /**
     * Private constructor. Use static execute() method instead.
     */
    private SwingWorkerTask(
            Supplier<T> backgroundTask,
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
    
    /**
     * Execute a background task with automatic UI management.
     *
     * @param backgroundTask   The task to execute in background (returns T)
     * @param onSuccess        Callback executed on EDT when task succeeds
     * @param onError          Callback executed on EDT when task fails
     * @param buttonToDisable  Button to disable during execution (can be null)
     * @param progressBar      Progress bar to show indeterminate progress (can be null)
     * @param <T>              Result type
     */
    public static <T> void execute(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            JButton buttonToDisable,
            JProgressBar progressBar) {
        
        SwingWorkerTask<T> task = new SwingWorkerTask<>(
            backgroundTask, onSuccess, onError, buttonToDisable, progressBar
        );
        task.start();
    }
    
    /**
     * Start the SwingWorker execution.
     */
    private void start() {
        // Disable button if provided
        if (buttonToDisable != null) {
            buttonToDisable.setEnabled(false);
        }
        
        // Show progress bar if provided
        if (progressBar != null) {
            progressBar.setIndeterminate(true);
            progressBar.setVisible(true);
        }
        
        // Create and execute SwingWorker
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.get();
            }
            
            @Override
            protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (Exception e) {
                    if (onError != null) {
                        // Unwrap ExecutionException to get the actual cause
                        Throwable cause = e.getCause();
                        if (cause instanceof Exception) {
                            onError.accept((Exception) cause);
                        } else {
                            onError.accept(e);
                        }
                    }
                } finally {
                    // Re-enable button
                    if (buttonToDisable != null) {
                        buttonToDisable.setEnabled(true);
                    }
                    
                    // Hide progress bar
                    if (progressBar != null) {
                        progressBar.setIndeterminate(false);
                        progressBar.setVisible(false);
                    }
                }
            }
        };
        
        worker.execute();
    }
}
