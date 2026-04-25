package com.examplatform.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // allows @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    // ── Public routes (no JWT required) ─────────────────────────────────────────
    private static final String[] PUBLIC_ROUTES = {
            "/api/auth/register",
            "/api/auth/login"
    };

    // ── Student-only routes ──────────────────────────────────────────────────────
    // (professor routes will be added in V2)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with our config below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Route-level authorization
                .authorizeHttpRequests(auth -> auth

                        // Public: signup and login
                        .requestMatchers(PUBLIC_ROUTES).permitAll()

                        // Student-only routes
                        .requestMatchers(HttpMethod.GET,  "/api/exams/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/attempts/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT,  "/api/attempts/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET,  "/api/attempts/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET,  "/api/results/**").hasRole("STUDENT")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Stateless — no session, no cookies. JWT is the only state.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Plug in our custom auth provider
                .authenticationProvider(authenticationProvider())

                // Run JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Authentication provider ──────────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Password encoder ─────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 12 = good balance
    }

    // ── CORS config ──────────────────────────────────────────────────────────────
    // During development React runs on localhost:3000, Spring on localhost:8080

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",      // React dev server
                "http://localhost:8080"       // Vite dev server (if you use Vite)
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        config.setAllowCredentials(true);  // needed for Authorization header
        config.setMaxAge(3600L);           // preflight cache for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}