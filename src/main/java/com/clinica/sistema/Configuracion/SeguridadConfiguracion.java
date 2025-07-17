package com.clinica.sistema.Configuracion;

import com.clinica.sistema.Servicio.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfiguracion {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Configura el AuthenticationManager para usar el servicio de usuario personalizado y el codificador de contraseñas.
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Permite acceso sin autenticación a estas rutas y recursos estáticos.
                .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/images/**").permitAll()
                // Cualquier otra petición debe ser autenticada.
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Especifica la URL de la página de inicio de sesión personalizada.
                .loginPage("/login")
                // Define el nombre del parámetro para el nombre de usuario (correo).
                .usernameParameter("correo")
                // Define el nombre del parámetro para la contraseña.
                .passwordParameter("contraseña")
                // Redirecciona a la raíz ("/") después de un inicio de sesión exitoso.
                .defaultSuccessUrl("/", true)
                // Redirecciona a "/login?error" en caso de inicio de sesión fallido.
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                // Define la URL para el cierre de sesión.
                .logoutUrl("/logout")
                // Redirecciona a "/login?logout" después de un cierre de sesión exitoso.
                .logoutSuccessUrl("/login?logout")
                // Invalida la sesión HTTP actual y elimina la cookie JSESSIONID al cerrar sesión.
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}