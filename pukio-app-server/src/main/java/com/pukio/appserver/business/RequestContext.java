package com.pukio.appserver.business;

public class RequestContext {
    private static final ThreadLocal<String> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> role = new ThreadLocal<>();
    private static final ThreadLocal<String> ipAddress = new ThreadLocal<>();

    public static void setUserId(String user) {
        userId.set(user);
    }

    public static String getUserId() {
        return userId.get();
    }

    public static void setRole(String userRole) {
        role.set(userRole);
    }

    public static String getRole() {
        return role.get();
    }

    public static void setIpAddress(String ip) {
        ipAddress.set(ip);
    }

    public static String getIpAddress() {
        return ipAddress.get();
    }

    public static void clear() {
        userId.remove();
        role.remove();
        ipAddress.remove();
    }
}
