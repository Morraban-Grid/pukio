package com.pukio.util;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtil {

    private SecurityUtil() {}

    /** Genera un hash BCrypt del texto en claro (factor de costo 12). */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /** Verifica que el texto en claro corresponda al hash BCrypt almacenado. */
    public static boolean verificar(String plain, String hash) {
        return BCrypt.checkpw(plain, hash);
    }
}
