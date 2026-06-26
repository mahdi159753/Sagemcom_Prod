package com.alibou.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static com.alibou.security.user.Permission.*;
import static com.alibou.security.user.Role.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    // PUBLIC: only auth endpoints (login / register / refresh)
    private static final String[] WHITE_LIST_URL = {
            "/api/v1/auth/authenticate",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/v2/api-docs", "/v3/api-docs", "/v3/api-docs/**",
            "/swagger-resources", "/swagger-resources/**",
            "/configuration/ui", "/configuration/security",
            "/swagger-ui/**", "/webjars/**", "/swagger-ui.html",
            "/ws-notifications", "/ws-notifications/**",
            "/api/v1/chat/files/**",
            "/api/v1/fi-guide/images/**",
            "/uploads/**",
            "/error"
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider  authenticationProvider;
    private final LogoutHandler           logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(req -> req

                        // ── PUBLIC ──────────────────────────────────────────────────
                        .requestMatchers(WHITE_LIST_URL).permitAll()
                        .requestMatchers(OPTIONS, "/**").permitAll()

                        // ── USER MANAGEMENT (/api/v1/users/**) ── ADMIN ONLY ─────────
                        .requestMatchers(GET,    "/api/v1/users/**").hasRole(ADMIN.name())
                        .requestMatchers(PUT,    "/api/v1/users/**").hasRole(ADMIN.name())
                        .requestMatchers(DELETE, "/api/v1/users/**").hasRole(ADMIN.name())
                        .requestMatchers(PATCH,  "/api/v1/users/**").authenticated() // change-password: any user

                        // ── ADMIN ONLY (/api/v1/admin/**) ───────────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole(ADMIN.name())
                        .requestMatchers(GET,    "/api/v1/admin/**").hasAuthority(ADMIN_READ.name())
                        .requestMatchers(POST,   "/api/v1/admin/**").hasAuthority(ADMIN_CREATE.name())
                        .requestMatchers(PUT,    "/api/v1/admin/**").hasAuthority(ADMIN_UPDATE.name())
                        .requestMatchers(DELETE, "/api/v1/admin/**").hasAuthority(ADMIN_DELETE.name())

                        // ── LIGNES DE PRODUCTION (/api/v1/lignes/**) ─────────────────
                        .requestMatchers(POST,   "/api/v1/lignes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(PUT,    "/api/v1/lignes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(DELETE, "/api/v1/lignes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(GET,    "/api/v1/lignes/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── POSTES DE TRAVAIL (/api/v1/postes/**) ────────────────────
                        .requestMatchers(POST,   "/api/v1/postes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(PUT,    "/api/v1/postes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(DELETE, "/api/v1/postes/**").hasAnyRole(ADMIN.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers(GET,    "/api/v1/postes/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── FICHES D'INSTRUCTION (/api/v1/fiches/**) ─────────────────
                        .requestMatchers(POST,   "/api/v1/fiches/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(PUT,    "/api/v1/fiches/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(DELETE, "/api/v1/fiches/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(GET,    "/api/v1/fiches/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── FI-GUIDE (/api/v1/fi-guide/**) ───────────────────────────
                        .requestMatchers(POST,   "/api/v1/fi-guide/upload").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(GET,    "/api/v1/fi-guide/sessions").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name(), RESPONSABLE_PRODUCTION.name())
                        .requestMatchers("/api/v1/fi-guide/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )


                        // ── PRODUITS (/api/v1/produits/**) ───────────────────────────
                        .requestMatchers(POST,   "/api/v1/produits/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(PUT,    "/api/v1/produits/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(DELETE, "/api/v1/produits/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(GET,    "/api/v1/produits/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── INDICATEURS / SAISIE KPI (/api/v1/indicateurs/**) ────────
                        .requestMatchers(POST,   "/api/v1/indicateurs/**").hasAnyRole(
                                ADMIN.name(), PREPARATEUR.name(), RESPONSABLE_PRODUCTION.name()
                        )
                        .requestMatchers(PUT,    "/api/v1/indicateurs/**").hasAnyRole(
                                ADMIN.name(), PREPARATEUR.name()
                        )
                        .requestMatchers(GET,    "/api/v1/indicateurs/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── KPI DASHBOARD (/api/v1/kpi/**) ───────────────────────────
                        .requestMatchers(GET, "/api/v1/kpi/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(),
                                INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )

                        // ── NON-CONFORMITES (/api/v1/nc/**) ──────────────────────────
                        .requestMatchers(POST,   "/api/v1/nc/**").hasAnyRole(
                                ADMIN.name(), INGENIEUR_QUALITE.name(), PREPARATEUR.name()
                        )
                        .requestMatchers(PUT,    "/api/v1/nc/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(DELETE, "/api/v1/nc/**").hasAnyRole(ADMIN.name(), INGENIEUR_QUALITE.name())
                        .requestMatchers(GET,    "/api/v1/nc/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(), INGENIEUR_QUALITE.name(),PREPARATEUR.name()
                        )

                        // ── RAPPORTS (/api/v1/rapports/**) ───────────────────────────
                        .requestMatchers("/api/v1/rapports/**").hasAnyRole(
                                ADMIN.name(), RESPONSABLE_PRODUCTION.name(), INGENIEUR_QUALITE.name()
                        )

                        // ── EVERYTHING ELSE: authenticated ───────────────────────────
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout.logoutUrl("/api/v1/auth/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessHandler((req, res, auth) -> SecurityContextHolder.clearContext())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
