package com.pukio.service;

import com.pukio.dao.DataWarehouseDAO;
import java.sql.SQLException;
import java.util.List;

public class DataWarehouseService {

    private final DataWarehouseDAO dao = new DataWarehouseDAO();

    public int generarDWH() throws SQLException   { return dao.cargarDWH(); }
    public int generarCrossTab() throws SQLException { return dao.generarCrossTab(); }
    public List<Object[]> verCrossTab() throws SQLException { return dao.obtenerCrossTab(); }
    public int totalRegistrosDWH() throws SQLException { return dao.totalRegistrosDWH(); }
}
