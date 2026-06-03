package com.pukio.posclient.ui.util;

/**
 * Interfaz para paneles que pueden ser refrescados.
 * Usada por MainFrame para implementar el atajo F5.
 * TASK-E2-26r
 */
public interface RefreshablePanel {
    /**
     * Refresca el contenido del panel, típicamente recargando datos del servidor.
     */
    void refresh();
}
