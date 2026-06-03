package com.pukio.posclient.client;

/**
 * Static utility class for managing user session context.
 * Stores authentication token and user information after successful login.
 */
public class SessionContext {
    private static String token;
    private static String userId;
    private static String username;
    private static String role;
    private static String storeId;
    private static String storeName;
    private static String shiftId;

    private SessionContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the session context after successful login.
     */
    public static void setSession(String token, String userId, String username, 
                                   String role, String storeId, String storeName, String shiftId) {
        SessionContext.token = token;
        SessionContext.userId = userId;
        SessionContext.username = username;
        SessionContext.role = role;
        SessionContext.storeId = storeId;
        SessionContext.storeName = storeName;
        SessionContext.shiftId = shiftId;
    }

    /**
     * Clear the session context (logout).
     */
    public static void clearSession() {
        token = null;
        userId = null;
        username = null;
        role = null;
        storeId = null;
        storeName = null;
        shiftId = null;
    }

    /**
     * Get the authentication token.
     */
    public static String getToken() {
        return token;
    }

    /**
     * Get the user ID.
     */
    public static String getUserId() {
        return userId;
    }

    /**
     * Get the username.
     */
    public static String getUsername() {
        return username;
    }

    /**
     * Get the user role.
     */
    public static String getRole() {
        return role;
    }

    /**
     * Get the store ID.
     */
    public static String getStoreId() {
        return storeId;
    }

    /**
     * Get the store name.
     */
    public static String getStoreName() {
        return storeName;
    }

    /**
     * Get the shift ID.
     */
    public static String getShiftId() {
        return shiftId;
    }

    /**
     * Check if a user is logged in.
     */
    public static boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
