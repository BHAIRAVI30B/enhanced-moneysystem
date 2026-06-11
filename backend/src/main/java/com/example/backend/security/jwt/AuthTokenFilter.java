package com.example.backend.security.jwt;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.backend.entities.UserEntity;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.service.UserDetailsImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    // NEW: UserRepository injected to validate session
    public AuthTokenFilter(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                String sessionId = jwtUtils.getSessionIdFromJwtToken(jwt);

                // NEW: check session against DB
                UserEntity user = userRepository.findByUsername(username);

                if (user == null || !sessionId.equals(user.getSessionId())) {
                    // Session was invalidated by a new login on another device
                    logger.warn("Session mismatch for user '{}' — likely logged in on another device", username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"message\": \"Your session was closed because a new session was opened on another device. Please log in again.\"}"
                    );
                    return;
                }

                String accountId = jwtUtils.getAccountIdFromJwtToken(jwt);
                List<String> roles = jwtUtils.getRolesFromJwtToken(jwt);

                logger.info("Parsed JWT: username={}, accountId={}, roles={}", username, accountId, roles);

                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UserDetailsImpl userDetails = new UserDetailsImpl(
                        null,
                        username,
                        null,
                        authorities,
                        accountId
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}