package br.com.fiap.mercadomvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/registro", "/css/**", "/images/**", "/webjars/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/mercado/novo", "/mercado/*/editar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/mercado", "/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/mercado", "/mercado/*").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/mercado", "/api/mercado/*").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/mercado", "/api/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/mercado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/compras").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/compras", "/api/compras/*")
                        .hasAnyRole("ADMIN", "CLIENTE")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        // JSON clients such as Postman do not receive Thymeleaf CSRF tokens.
                        // The MVC form routes remain protected by CSRF.
                        .ignoringRequestMatchers("/api/**"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/mercado", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
