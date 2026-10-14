
package com.example.LoanMatrix.config;

import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.service.authentication.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository,RateLimitingFilter rateLimitingFilter) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userRepository);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )


                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        rateLimitingFilter,
                        JwtAuthenticationFilter.class
                )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, ex) ->
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Authentication required"
                        )
                )
                .accessDeniedHandler((request, response, ex) ->
                        response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "Access denied"
                        )
                )
        );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter>
    rateLimitingFilterRegistration(RateLimitingFilter rateLimitingFilter) {

        FilterRegistrationBean<RateLimitingFilter> registration =
                new FilterRegistrationBean<>(rateLimitingFilter);

        registration.setEnabled(false);

        return registration;
    }
}
