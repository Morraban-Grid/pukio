package com.pukio.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatoUtil {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es","PE"));
    private static final SimpleDateFormat FECHA_CORTA = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat FECHA_LARGA = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private FormatoUtil() {}

    public static String moneda(double valor) {
        return MONEDA.format(valor);
    }

    public static String fechaCorta(Date d) {
        return d == null ? "" : FECHA_CORTA.format(d);
    }

    public static String fechaLarga(Date d) {
        return d == null ? "" : FECHA_LARGA.format(d);
    }

    public static String porcentaje(double valor) {
        return String.format("%.1f%%", valor * 100);
    }
}
