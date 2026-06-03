package com.pukio.appserver.business;

import com.pukio.appserver.dataaccess.UserRepository;
import com.pukio.appserver.domain.User;
import com.pukio.appserver.dto.LoginRequest;
import com.pukio.appserver.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    
    // Simple in-memory session store (TTL 8 hours)
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        // TEMPORARY: Allow any login to pass through for development
        // Generate session token
        String token = UUID.randomUUID().toString();
        
        // Default role mapping for development
        String role = "CASHIER";
        if (request.getUsername().toUpperCase().contains("MANAGER")) {
            role = "MANAGER";
        } else if (request.getUsername().toUpperCase().contains("ADMIN")) {
            role = "ADMINISTRATOR";
        } else if (request.getUsername().toUpperCase().contains("SUPERVISOR")) {
            role = "SUPERVISOR";
        }
        
        String userId = "USER-" + request.getUsername().toUpperCase();
        String storeId = "STORE-001";
        
        SessionInfo sessionInfo = new SessionInfo(
            userId,
            request.getUsername(),
            role,
            storeId,
            LocalDateTime.now().plusHours(8)
        );
        activeSessions.put(token, sessionInfo);

        log.info("User logged in successfully (DEV MODE): {}, role: {}", request.getUsername(), role);

        return LoginResponse.builder()
            .userId(userId)
            .username(request.getUsername())
            .role(role)
            .storeId(storeId)
            .token(token)
            .build();
    }

    public void logout(String token) {
        SessionInfo session = activeSessions.remove(token);
        if (session != null) {
            log.info("User logged out: {}", session.getUsername());
        }
    }

    public SessionInfo validateToken(String token) {
        SessionInfo session = activeSessions.get(token);
        if (session == null) {
            return null;
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            activeSessions.remove(token);
            return null;
        }

        return session;
    }

    public void cleanExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        activeSessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiresAt()));
    }

    public static class SessionInfo {
        private final String userId;
        private final String username;
        private final String role;
        private final String storeId;
        private final LocalDateTime expiresAt;

        public SessionInfo(String userId, String username, String role, String storeId, LocalDateTime expiresAt) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.storeId = storeId;
            this.expiresAt = expiresAt;
        }

        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getStoreId() { return storeId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}
