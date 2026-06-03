package com.pukio.posclient.session;

/**
 * Manages the current user session state.
 * This is a static context holder for session information.
 */
public class SessionContext {
    
    private static String token;
    private static String userId;
    private static String username;
    private static String role;
    private static String storeId;
    private static String currentShiftId;
    private static String cashierId;
    
    /**
     * Set the session information after successful login.
     * 
     * @param token JWT token
     * @param userId User identifier
     * @param username Username
     * @param role User role (cashier, supervisor, manager, administrator, auditor)
     * @param storeId Store identifier
     */
    public static void setSession(String token, String userId, String username, String role, String storeId) {
        SessionContext.token = token;
        SessionContext.userId = userId;
        SessionContext.username = username;
        SessionContext.role = role;
        SessionContext.storeId = storeId;
    }
    
    /**
     * Clear the session (used on logout).
     */
    public static void clearSession() {
        token = null;
        userId = null;
        username = null;
        role = null;
        storeId = null;
        currentShiftId = null;
        cashierId = null;
    }
    
    /**
     * Get the current JWT token.
     * 
     * @return JWT token or null if not logged in
     */
    public static String getToken() {
        return token;
    }
    
    /**
     * Get the current user's role.
     * 
     * @return Role or null if not logged in
     */
    public static String getRole() {
        return role;
    }
    
    /**
     * Get the current user's store ID.
     * 
     * @return Store ID or null if not logged in
     */
    public static String getStoreId() {
        return storeId;
    }
    
    /**
     * Get the current user ID.
     * 
     * @return User ID or null if not logged in
     */
    public static String getUserId() {
        return userId;
    }
    
    /**
     * Get the current username.
     * 
     * @return Username or null if not logged in
     */
    public static String getUsername() {
        return username;
    }
    
    /**
     * Check if a user is currently logged in.
     * 
     * @return true if logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
    
    /**
     * Get the current shift ID.
     * 
     * @return Current shift ID or null if not set
     */
    public static String getCurrentShiftId() {
        return currentShiftId;
    }
    
    /**
     * Get the shift ID (alias for getCurrentShiftId).
     * 
     * @return Shift ID or null if not set
     */
    public static String getShiftId() {
        return currentShiftId;
    }
    
    /**
     * Set the current shift ID.
     * 
     * @param shiftId Shift identifier
     */
    public static void setCurrentShiftId(String shiftId) {
        currentShiftId = shiftId;
    }
    
    /**
     * Get the cashier ID.
     * 
     * @return Cashier ID or null if not set
     */
    public static String getCashierId() {
        return cashierId;
    }
    
    /**
     * Set the cashier ID.
     * 
     * @param id Cashier identifier
     */
    public static void setCashierId(String id) {
        cashierId = id;
    }
}
