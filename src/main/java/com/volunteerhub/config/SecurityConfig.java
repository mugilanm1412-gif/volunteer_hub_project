package com.volunteerhub.config;

import com.volunteerhub.security.CustomUserDetailsService;
import com.volunteerhub.security.JsonAccessDeniedHandler;
import com.volunteerhub.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Enterprise Spring Security configuration with role-based access control,
 * session-based authentication, CSRF cookie repository, and JSON error handlers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JsonAuthenticationEntryPoint authenticationEntryPoint,
                          JsonAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .csrf(csrf -> {
                org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
                    new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName(null);
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers(
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/register/**",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password"
                    );
            })
            .addFilterAfter(new com.volunteerhub.security.CsrfCookieFilter(), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(authenticationEntryPoint,
                    request -> request.getRequestURI().startsWith("/api/"))
                .defaultAccessDeniedHandlerFor(accessDeniedHandler,
                    request -> request.getRequestURI().startsWith("/api/"))
            )
            .authorizeHttpRequests(auth -> auth
                // Public Health and Static Assets
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/assets/**", "/favicon.ico").permitAll()

                // Public Authentication Endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Public Event Endpoints (Listing and details)
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/{id:[0-9]+}").permitAll()

                // Role-Specific Endpoints
                .requestMatchers("/api/volunteers/**").hasRole("VOLUNTEER")
                .requestMatchers("/api/ngos/**").hasRole("NGO")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Authenticated General Endpoints
                .requestMatchers("/api/users/**", "/api/notifications/**").authenticated()

                // Default rule for all other endpoints
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
