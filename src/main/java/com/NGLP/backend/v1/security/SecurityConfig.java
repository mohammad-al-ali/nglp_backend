package com.NGLP.backend.v1.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 1. تفعيل CORS من الـ Bean الموجود بالأسفل
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. السماح بتحميل إطارات H2 Console (مهم جداً لعرض واجهة قاعدة البيانات)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 3. جعل النظام بدون جلسات (Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 3. ترتيب الصلاحيات في بلوك واحد
                .authorizeHttpRequests(auth -> auth
                        // 1. السماح بطلبات الاستكشاف (CORS Preflight) من المتصفح
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. السماح لمسارات المصادقة (Auth)
                        .requestMatchers("/api/v1/auth/", "/auth/").permitAll()

                        // 3. السماح بمسارات التصنيفات (Categories) - بالمسارين تحسباً للـ Context Path
                        .requestMatchers("/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/courses/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/roles").permitAll()
                        .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                        // السماح لـ Swagger والأخطاء
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        // السماح بقراءة الفيديوهات
                        .requestMatchers("/uploads/**").permitAll()
                        // السماح لأي طلب من نوع OPTIONS (مهم جداً لتجنب 403 من المتصفح)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // أي طلب آخر مرفوض بدون تسجيل دخول
                        .anyRequest().permitAll()
                        );

        return http.build();
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/h2-console/**");
    }

    @Bean
    public org.springframework.boot.web.servlet.ServletRegistrationBean<?> h2ConsoleServletRegistration() {
        try {
            Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            jakarta.servlet.Servlet servlet = (jakarta.servlet.Servlet) servletClass.getDeclaredConstructor().newInstance();
            org.springframework.boot.web.servlet.ServletRegistrationBean<?> registration =
                    new org.springframework.boot.web.servlet.ServletRegistrationBean<>(servlet);
            registration.addUrlMappings("/h2-console/*");
            return registration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to register H2 console servlet dynamically", e);
        }
    }

    // 🌟 هذا هو المكان الصحيح لتعريف الـ CORS لكي يراه Spring Security
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // يمكننا تركها true تحسباً لأي متطلبات مستقبلية
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
