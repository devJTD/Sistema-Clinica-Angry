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
    private final CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private final PasswordEncoder passwordEncoder;

    // Inyecta tu CustomLogoutHandler
    private final CustomLogoutHandler customLogoutHandler;

    // Constructor que inyecta los servicios necesarios, incluyendo CustomLogoutHandler.
    public SeguridadConfiguracion(CustomUserDetailsService customUserDetailsService, 
                                  PasswordEncoder passwordEncoder,
                                  CustomLogoutHandler customLogoutHandler) { // <-- Añade CustomLogoutHandler aqui
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.customLogoutHandler = customLogoutHandler; // <-- Inicializalo
    }

    // Configura el AuthenticationManager para usar el servicio de usuario personalizado y el codificador de contraseñas.
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Permite acceso sin autenticacion a estas rutas y recursos estaticos.
                .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/images/**").permitAll()
                // Cualquier otra peticion debe ser autenticada.
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Especifica la URL de la pagina de inicio de sesion personalizada.
                .loginPage("/login")
                // Define el nombre del parametro para el nombre de usuario (correo).
                .usernameParameter("correo")
                // Define el nombre del parametro para la contraseña.
                .passwordParameter("contraseña")
                // Redirecciona a la raiz ("/") despues de un inicio de sesion exitoso.
                .defaultSuccessUrl("/", true)
                // Redirecciona a "/login?error" en caso de inicio de sesion fallido.
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                // Define la URL para el cierre de sesion.
                .logoutUrl("/logout")
                .addLogoutHandler(customLogoutHandler) // <-- AQUI se añade tu CustomLogoutHandler
                // Redirecciona a "/login?logout" despues de un cierre de sesion exitoso.
                .logoutSuccessUrl("/login?logout")
                // Invalida la sesion HTTP actual y elimina la cookie JSESSIONID al cerrar sesion.
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}