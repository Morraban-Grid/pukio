package com.pukio.appserver.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pukio.appserver.business.AuthService;
import com.pukio.appserver.business.RequestContext;
import com.pukio.appserver.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // TEMPORARY: Disable authentication filter for all endpoints during development
        log.debug("SessionFilter bypassed for development - path: {}", path);
        filterChain.doFilter(request, response);
        
        /* ORIGINAL CODE - RESTORE FOR PRODUCTION
        // Skip authentication for login endpoint
        if (path.equals("/api/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendUnauthorized(response, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(7);
            AuthService.SessionInfo session = authService.validateToken(token);

            if (session == null) {
                sendUnauthorized(response, "Invalid or expired token");
                return;
            }

            // Set user context
            RequestContext.setUserId(session.getUserId());
            RequestContext.setRole(session.getRole());
            RequestContext.setIpAddress(request.getRemoteAddr());

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
        */
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .message("Unauthorized: " + message)
            .path("")
            .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
