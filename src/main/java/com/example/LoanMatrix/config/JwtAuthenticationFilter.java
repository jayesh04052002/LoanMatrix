
package com.example.LoanMatrix.config;

import com.example.LoanMatrix.entity.User;
import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.service.authentication.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // If no Bearer token is supplied, continue.
        // Spring Security will decide whether authentication is required.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Validate the token and extract the user's email.
            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid JWT token"
                );
                return;
            }

            // Avoid replacing an existing authentication.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load the user together with their role.
                User user = userRepository.findWithRoleByEmail(email);

                if (user == null || user.getRole() == null
                        || user.getRole().getRoleName() == null) {

                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "User or role not found"
                    );
                    return;
                }

                String roleName = user.getRole().getRoleName();

                // Spring Security role authorities conventionally use ROLE_.
                String authority = roleName.startsWith("ROLE_")
                        ? roleName
                        : "ROLE_" + roleName;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired JWT token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
